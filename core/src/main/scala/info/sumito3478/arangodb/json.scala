package info.sumito3478.arangodb

package object json {
  // simple and ad hoc Json tree. Not intended for general use.
  sealed trait JsonValue
  case class JsonBoolean(value: Boolean) extends JsonValue
  case class JsonNumber(value: BigDecimal) extends JsonValue
  case class JsonString(value: String) extends JsonValue
  case class JsonArray(value: Seq[JsonValue]) extends JsonValue
  case class JsonObject(value: Map[String, JsonValue]) extends JsonValue
}