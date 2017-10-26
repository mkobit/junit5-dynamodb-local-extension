package com.mkobit.junit.jupiter.aws.dynamodb;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBStreams;
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
import com.amazonaws.services.dynamodbv2.local.shared.access.AmazonDynamoDBLocal;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Creates a {@link AmazonDynamoDBLocal} instance and provides it to the tests.
 */
class EmbeddedDynamoDBExtension
    implements AfterEachCallback, BeforeAllCallback, AfterAllCallback, ParameterResolver {
  private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedDynamoDBExtension.class);
  private static final String SQLLITE4JAVA_LIB_PATH_KEY = "sqlite4java.library.path";

  private static final Predicate<String> PATH_ELEMENT_FILTER = (element) -> element.contains("sqlite")
      && (element.endsWith("dll") || element.endsWith("so") || element.endsWith("dylib"));

  @Override
  public void beforeAll(final ExtensionContext context) throws Exception {
    final ExtensionContext.Store store = getStore(context);
    final Path tempDirectory = store.getOrComputeIfAbsent(
        context,
        ThrowingFunction.wrap(
            "Could not create temp native-libraries directory",
            extensionContext -> Files.createTempDirectory("native-libraries")
        ), Path.class
    );
    System.setProperty(SQLLITE4JAVA_LIB_PATH_KEY, tempDirectory.toAbsolutePath().toString());
    Stream.of(System.getProperty("java.class.path", "."))
          .map(classPath -> classPath.split(System.getProperty("path.separator")))
          .flatMap(Arrays::stream)
          .filter(PATH_ELEMENT_FILTER)
          .map(File::new)
          .map(File::toPath)
          .forEach(
              ThrowingConsumer.wrap(
                  libraryPath -> "Could not copy file " + libraryPath + " to " + tempDirectory.resolve(libraryPath.getFileName()),
                  libraryPath -> Files.copy(libraryPath, tempDirectory.resolve(libraryPath.getFileName()))
              )
          );
  }

  @Override
  public void afterAll(final ExtensionContext context) throws Exception {
    final ExtensionContext.Store store = getStore(context);
    final Path tempNativeLibraries = store.remove(context, Path.class);
    if (tempNativeLibraries != null) {
      LOGGER.debug("Deleting all native library files at {}", tempNativeLibraries);
      Files.walk(tempNativeLibraries)
           .sorted(Comparator.reverseOrder())
           .peek(path -> LOGGER.debug("Deleting file/directory located at " + path))
           .forEach(ThrowingConsumer.wrap(Files::delete));
    }
  }

  @Override
  public void afterEach(final ExtensionContext context) throws Exception {
    LOGGER.debug("Shutting down Dynamo DB instance for {}", context.getElement());
    final ExtensionContext.Store store = getStore(context);
    final AmazonDynamoDBLocal dynamoDBLocal = store.remove(context, AmazonDynamoDBLocal.class);
    if (dynamoDBLocal != null) {
      dynamoDBLocal.shutdown();
    }
  }

  @Override
  public boolean supportsParameter(
      final ParameterContext parameterContext,
      final ExtensionContext extensionContext
  ) throws ParameterResolutionException {
    final Class<?> type = parameterContext.getParameter().getType();
    return type == AmazonDynamoDBLocal.class
        || type == AmazonDynamoDB.class
        || type == AmazonDynamoDBStreams.class;
  }

  @Override
  public Object resolveParameter(
      final ParameterContext parameterContext,
      final ExtensionContext extensionContext
  ) throws ParameterResolutionException {
    final ExtensionContext.Store store = getStore(extensionContext);
    final AmazonDynamoDBLocal dynamoDBLocal = store.getOrComputeIfAbsent(
        extensionContext,
        context -> DynamoDBEmbedded.create(),
        AmazonDynamoDBLocal.class
    );
    final Class<?> type = parameterContext.getParameter().getType();
    if (type == AmazonDynamoDBLocal.class) {
      return dynamoDBLocal;
    } else if (type == AmazonDynamoDB.class) {
      return dynamoDBLocal.amazonDynamoDB();
    } else if (type == AmazonDynamoDBStreams.class) {
      return dynamoDBLocal.amazonDynamoDBStreams();
    } else {
      throw new ParameterResolutionException("Unsupported type " + type.getCanonicalName() + " not supported");
    }
  }

  private ExtensionContext.Store getStore(final ExtensionContext context) {
    return context.getStore(
        ExtensionContext.Namespace.create(
            EmbeddedDynamoDBExtension.class,
            context
        )
    );
  }

  @FunctionalInterface
  private interface ThrowingConsumer<T> {
    void accept(T t) throws Exception;

    static <T> Consumer<T> wrap(final ThrowingConsumer<T> consumer) {
      return wrap("Error processing consumer", consumer);
    }

    static <T> Consumer<T> wrap(final String message, final ThrowingConsumer<T> consumer) {
      return (T t) -> {
        try {
          consumer.accept(t);
        } catch (Exception exception) {
          throw new RuntimeException(message, exception);
        }
      };
    }

    static <T> Consumer<T> wrap(
        final Function<T, String> messageFunction,
        final ThrowingConsumer<T> consumer
    ) {
      return (T t) -> {
        try {
          consumer.accept(t);
        } catch (Exception exception) {
          throw new RuntimeException(messageFunction.apply(t), exception);
        }
      };
    }
  }

  @FunctionalInterface
  private interface ThrowingFunction<T, R> {
    R apply(T t) throws Exception;

    static <T, R> Function<T, R> wrap(ThrowingFunction<T, R> consumer) {
      return wrap("Error processing consumer", consumer);
    }

    static <T, R> Function<T, R> wrap(
        final String message,
        final ThrowingFunction<T, R> function
    ) {
      return (T t) -> {
        try {
          return function.apply(t);
        } catch (Exception exception) {
          throw new RuntimeException(message, exception);
        }
      };
    }

    static <T, R> Function<T, R> wrap(
        final Function<T, String> messageFunction,
        final ThrowingFunction<T, R> function
    ) {
      return (T t) -> {
        try {
          return function.apply(t);
        } catch (Exception exception) {
          throw new RuntimeException(messageFunction.apply(t), exception);
        }
      };
    }
  }
}
