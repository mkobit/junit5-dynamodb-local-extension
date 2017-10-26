package com.mkobit.junit.jupiter.aws.dynamodb

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBStreams
import com.amazonaws.services.dynamodbv2.local.shared.access.AmazonDynamoDBLocal
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest
import com.amazonaws.services.dynamodbv2.model.GetItemRequest
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement
import com.amazonaws.services.dynamodbv2.model.KeyType
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput
import com.amazonaws.services.dynamodbv2.model.PutItemRequest
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

// See https://forums.aws.amazon.com/thread.jspa?threadID=217222
// See https://github.com/awslabs/aws-dynamodb-examples/blob/master/src/test/java/com/amazonaws/services/dynamodbv2/local/embedded/DynamoDBEmbeddedTest.java#L44
@DynamoDBLocal
internal class EmbeddedDynamoDBExtensionTest {

  @Test
  internal fun `can execute extension without resolving any parameters`() {
  }

  @Test
  internal fun `can resolve AmazonDynamoDBLocal`(amazonDynamoDBLocal: AmazonDynamoDBLocal) {
    assertThat(amazonDynamoDBLocal).isNotNull()
  }

  @Test
  internal fun `can resolved AmazonDynamoDB`(amazonDynamoDB: AmazonDynamoDB) {
    assertThat(amazonDynamoDB).isNotNull()
  }

  @Test
  internal fun `can resolve AmazonDynamoDBStreams`(amazonDynamoDBStreams: AmazonDynamoDBStreams) {
    assertThat(amazonDynamoDBStreams).isNotNull()
  }

  @Test
  internal fun `can resolve all all parameter types in single method`(
      amazonDynamoDBLocal: AmazonDynamoDBLocal,
      amazonDynamoDB: AmazonDynamoDB,
      amazonDynamoDBStreams: AmazonDynamoDBStreams
  ) {
    assertThat(listOf(amazonDynamoDBLocal, amazonDynamoDB, amazonDynamoDBStreams)).allSatisfy {
      assertThat(it).isNotNull()
    }
  }

  @Test
  internal fun `can shutdown AmazonDynamoDBLocal without extension failing`(amazonDynamoDBLocal: AmazonDynamoDBLocal) {
    assertThat(amazonDynamoDBLocal).isNotNull()
    amazonDynamoDBLocal.shutdown()
  }

  @Test
  internal fun `can create, write, and read a table`(amazonDynamoDB: AmazonDynamoDB) {
    val table = "myTable"
    val hashKey = "myHashKey"
    amazonDynamoDB.createTable(
        CreateTableRequest().apply {
          tableName = table
          setKeySchema(listOf(KeySchemaElement(hashKey, KeyType.HASH)))
          withProvisionedThroughput(ProvisionedThroughput(10L, 10L))
          withAttributeDefinitions(listOf(AttributeDefinition(hashKey, ScalarAttributeType.S)))
        }
    )
    val hashKeyValue = "stringValue"
    amazonDynamoDB.putItem(
        PutItemRequest(table, mapOf(hashKey to AttributeValue(hashKeyValue)))
    )
    val getItemResult = amazonDynamoDB.getItem(
        GetItemRequest(table, mapOf(hashKey to AttributeValue(hashKeyValue)))
    )
    assertThat(getItemResult.item).hasSize(1).hasEntrySatisfying(hashKey) {
      assertThat(it)
          .extracting(java.util.function.Function { it.s })
          .hasOnlyOneElementSatisfying {
        assertThat(it).isEqualTo(hashKeyValue)
      }
    }
  }
}
