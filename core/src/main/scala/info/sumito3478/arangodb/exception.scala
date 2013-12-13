package info.sumito3478.arangodb

package object exception {
  case class ArangoErrorResponse(error: Boolean, code: Int, errorNum: Int, errorMessage: String)
}