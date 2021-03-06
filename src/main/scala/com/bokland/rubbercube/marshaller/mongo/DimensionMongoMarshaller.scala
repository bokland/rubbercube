package com.bokland.rubbercube.marshaller.mongo

import com.bokland.rubbercube.Dimension
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.query.Imports._

/**
 * Created by remeniuk on 5/1/14.
 */
object DimensionMongoMarshaller extends MongoMarshaller[Dimension] {

  def marshal(obj: Dimension): DBObject = {
    val builder = MongoDBObject.newBuilder

    builder += "field" -> obj.fieldName
    obj.cubeId.foreach(cubeId => builder += "cubeId" -> cubeId)
    obj.alias.foreach(alias => builder += "alias" -> alias)

    builder.result()
  }

  def unmarshal(obj: DBObject) =
    Dimension(obj.as[String]("field"), obj.getAs[String]("cubeId"),
      obj.getAs[String]("alias"))

}
