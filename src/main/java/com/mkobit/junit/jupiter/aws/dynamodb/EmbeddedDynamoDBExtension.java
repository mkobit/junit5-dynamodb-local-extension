package com.mkobit.junit.jupiter.aws.dynamodb;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBStreams;
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
import com.amazonaws.services.dynamodbv2.local.shared.access.AmazonDynamoDBLocal;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EmbeddedDynamoDBExtension
    implements AfterEachCallback, ParameterResolver {

  private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedDynamoDBExtension.class);

  @Override
  public void afterEach(final ExtensionContext context) throws Exception {
    LOGGER.debug("Shutting down Dynamo DB instance for {}", context.getElement());
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
}
