package com.mkobit.junit.jupiter.aws.dynamodb;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use a Dynamo DB Local instance for testing.
 * @see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.html">Setting Up DynamoDB Local</a>
 */
@Inherited
@Target({
    ElementType.METHOD,
    ElementType.TYPE,
})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(EmbeddedDynamoDBExtension.class)
public @interface DynamoDBLocal {
}
