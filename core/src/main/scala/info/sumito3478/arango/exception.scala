package info.sumito3478.arango

package object exception {
  case class ArangoErrorResponse(error: Boolean, code: Int, errorNum: Int, errorMessage: String)
}