package info.sumito3478.arango

package object macros {
  import scala.language.experimental.macros
  import scala.reflect.macros.Context
  import scalaz._, Scalaz._
  import argonaut._, Argonaut._
  def casecodecImpl[A: c.WeakTypeTag](c: Context): c.Expr[CodecJson[A]] = {
    import c.universe._
    val `type` = weakTypeOf[A]
    val typeName = `type`.typeSymbol.name.decoded
    val companion = `type`.typeSymbol.companionSymbol
    val params = `type`.declarations.collectFirst { case m: MethodSymbol if m.isPrimaryConstructor => m }.get.paramss.head
    val paramNames = for (param <- params) yield s""" "${param.name}" """
    val casecodec = c.parse(s"casecodec${params.size} _")
    println(q"$casecodec($companion.apply, $companion.unapply)(..$paramNames)")
    c.Expr[CodecJson[A]] {
      //q"$casecodec($companion.apply, $companion.unapply)(..$paramNames)"
      c.parse(s"casecodec${params.size}($typeName.apply, $typeName.unapply)(${paramNames.mkString(",")})")
    }
  }
  def casecodec[A]: CodecJson[A] = macro casecodecImpl[A]
}