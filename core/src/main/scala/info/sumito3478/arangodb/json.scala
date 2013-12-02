package info.sumito3478.arangodb

import com.fasterxml.jackson
import jackson.databind._
import jackson.module.scala._
import jackson.module.scala.experimental._
import jackson.datatype.jsr353._
import javax.json._

package object json {
  private[this] val mapper = new ObjectMapper with ScalaObjectMapper
  mapper.registerModules(DefaultScalaModule, new JSR353Module)
  private[this] val prettyPrinter = mapper.writerWithDefaultPrettyPrinter

  def parse(x: String) = mapper.readValue[JsonValue](x)

  implicit class JsonValueW(val self: JsonValue) {
    def as[A: Manifest] = mapper.convertValue[A](self)
    def pretty = prettyPrinter.writeValueAsString(self)
  }
}