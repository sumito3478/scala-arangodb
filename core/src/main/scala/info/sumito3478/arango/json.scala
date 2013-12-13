package info.sumito3478.arango

package object json {
  // simple and ad hoc Json tree. Not intended for general use.
  sealed trait JValue
  case class JBoolean(value: Boolean) extends JValue
  case class JInt(value: Int) extends JValue
  case class JLong(value: Long) extends JValue
  case class JBigInt(value: BigInt) extends JValue
  case class JDouble(value: Double) extends JValue
  case class JBigDecimal(value: BigDecimal) extends JValue
  case class JString(value: String) extends JValue
  case class JArray(value: Seq[JValue]) extends JValue
  case class JObject(value: Map[String, JValue]) extends JValue
  case object JNull extends JValue
  case object JNothing extends JValue

  sealed trait NumericPrecisionOption
  object NumericPrecisionOption {
    case object Checked extends NumericPrecisionOption
    case object Ignored extends NumericPrecisionOption
  }
  import NumericPrecisionOption._

  private[this] val precisionOption = new scala.util.DynamicVariable[NumericPrecisionOption](Ignored)

  private[this] def overflowed(x: String) =
    throw new ArithmeticException(s"value $x cannot be stored in ArangoDB, since only double-precision floating point numbers are supported. setting precision to NumericPrecisionOption.Ignored will disable this checking.")

  private[this] object Jackson {
    import com.fasterxml.jackson
    import jackson.core._
    import jackson.databind._
    import jackson.databind.jsontype._
    import jackson.databind.ser.std._
    import jackson.databind.deser.std._
    import jackson.databind.`type`._
    import jackson.databind.module._

    private[this] object JValueSerializer extends StdSerializer[JValue](classOf[JValue]) {
      override def serialize(value: JValue, gen: JsonGenerator, provider: SerializerProvider): Unit = {
        value match {
          case JBoolean(x) => gen.writeBoolean(x)
          case JInt(x) => gen.writeNumber(x)
          case JLong(x) =>
            val d = x.toDouble
            precisionOption.value match {
              case Checked => {
                if (d.toLong == x) gen.writeNumber(d)
                else overflowed(x.toString)
              }
              case Ignored => gen.writeNumber(x)
            }
          case JBigInt(x) =>
            val d = x.toDouble
            precisionOption.value match {
              case Checked => {
                if (BigInt(d.toLong) == x) gen.writeNumber(d)
                else overflowed(x.toString)
              }
              case Ignored => gen.writeNumber(x.bigInteger)
            }
          case JDouble(x) => gen.writeNumber(x)
          case JBigDecimal(x) =>
            val d = x.toDouble
            precisionOption.value match {
              case Checked => {
                if (BigDecimal(d) == x) gen.writeNumber(d)
                else overflowed(x.toString)
              }
              case Ignored => gen.writeNumber(x.bigDecimal)
            }
          case JString(x) => gen.writeString(x)
          case JArray(xs) =>
            gen.writeStartArray
            for (x <- xs) serialize(x, gen, provider)
            gen.writeEndArray
          case JObject(xs) =>
            gen.writeStartObject
            for ((k, v) <- xs) {
              gen.writeFieldName(k)
              serialize(v, gen, provider)
            }
            gen.writeEndObject
          case JNull => gen.writeNull
          case JNothing =>
        }
      }
      // is this necessary?
      override def serializeWithType(value: JValue, gen: JsonGenerator, provider: SerializerProvider, typeSer: TypeSerializer): Unit = {
        typeSer.writeTypePrefixForScalar(value, gen)
        serialize(value, gen, provider)
        typeSer.writeTypeSuffixForScalar(value, gen)
      }
    }
    private[this] object JValueDeserializer extends StdDeserializer[JValue](classOf[JValue]) {
      override def deserialize(p: JsonParser, ctx: DeserializationContext): JValue = {
        import JsonToken._
        p.getCurrentToken match {
          case START_OBJECT =>
            def loop(xs: List[(String, JValue)]): JObject =
              p.nextToken match {
                case END_OBJECT => JObject(xs.reverse.toMap)
                case _ =>
                  val name = p.getCurrentName
                  p.nextToken
                  val value = deserialize(p, ctx)
                  loop((name, value) :: xs)
              }
            loop(List())
          case START_ARRAY =>
            def loop(xs: List[JValue]): JArray =
              p.nextToken match {
                case END_ARRAY => JArray(xs.reverse)
                case _ => loop(deserialize(p, ctx) :: xs)
              }
            loop(List())
          case VALUE_EMBEDDED_OBJECT => throw ctx.mappingException(classOf[JValue])
          case VALUE_FALSE => JBoolean(false)
          case VALUE_TRUE => JBoolean(true)
          case VALUE_NULL => JNull
          case VALUE_NUMBER_FLOAT =>
            BigDecimal(p.getDecimalValue) match {
              case x if x == BigDecimal(x.toDouble) /* x.isValidDouble doesn't work... */ => JDouble(x.toDouble)
              case x => JBigDecimal(x)
            }
          case VALUE_NUMBER_INT =>
            BigInt(p.getBigIntegerValue) match {
              case x if x.isValidInt => JInt(x.toInt)
              case x if x.isValidLong => JLong(x.toLong)
              case x => JBigInt(x)
            }
          case VALUE_STRING => JString(p.getText)
          case _ => throw ctx.mappingException(classOf[JValue])
        }
      }
      override def deserializeWithType(p: JsonParser, ctx: DeserializationContext, typeDeser: TypeDeserializer) = typeDeser.deserializeTypedFromScalar(p, ctx)
    }
    private[this] object Module extends SimpleModule(Version.unknownVersion) {
      addSerializer(JValueSerializer)
      setDeserializers(new SimpleDeserializers {
        override def findBeanDeserializer(ty: JavaType, config: DeserializationConfig, beanDesc: BeanDescription): JsonDeserializer[_] =
          if (classOf[JValue].isAssignableFrom(ty.getRawClass)) JValueDeserializer else null
        override def findCollectionDeserializer(ty: CollectionType, config: DeserializationConfig, beanDesc: BeanDescription, elementTypeDeserializer: TypeDeserializer, elementDeserializer: JsonDeserializer[_]): JsonDeserializer[_] =
          if (classOf[JArray].isAssignableFrom(ty.getRawClass)) JValueDeserializer else null
        override def findMapDeserializer(ty: MapType, config: DeserializationConfig, beanDesc: BeanDescription, keyDeserializer: KeyDeserializer, elementTypeDeserializer: TypeDeserializer,
          elementDeserializer: JsonDeserializer[_]): JsonDeserializer[_] =
          if (classOf[JObject].isAssignableFrom(ty.getRawClass)) JValueDeserializer else null
      })
    }
    import com.fasterxml.jackson.module.scala._
    import com.fasterxml.jackson.module.scala.experimental._
    private[json] val mapper = new ObjectMapper with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    mapper.registerModule(Module)
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  }
  def write[A: Manifest](x: A, precision: NumericPrecisionOption = Checked) =
    precisionOption.withValue(precision) {
      Jackson.mapper.writeValueAsString(x)
    }
  def read[A: Manifest](x: String) = Jackson.mapper.readValue[A](x)
  def convert[A: Manifest](x: Any, precision: NumericPrecisionOption = Ignored) =
    precisionOption.withValue(precision) {
      Jackson.mapper.convertValue[A](x)
    }
}