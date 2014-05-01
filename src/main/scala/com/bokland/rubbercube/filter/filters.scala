package com.bokland.rubbercube.filter

import com.bokland.rubbercube.Dimension

/**
 * Created by remeniuk on 5/1/14.
 */
case class gt(dimension: Dimension, value: Any) extends Filter with SingleDimension

case class gte(dimension: Dimension, value: Any) extends Filter with SingleDimension

case class lt(dimension: Dimension, value: Any) extends Filter with SingleDimension

case class lte(dimension: Dimension, value: Any) extends Filter with SingleDimension

case class eql(dimension: Dimension, value: Any) extends Filter with SingleDimension

case class neql(dimension: Dimension, value: Any) extends Filter with SingleDimension

case class in(dimension: Dimension, value: Any) extends Filter with SingleDimension

case class sequence(dimension: Dimension, value: Seq[String]*) extends Filter with SingleDimension

case class and(filters: Filter*) extends Filter with MultiDimensional

case class or(filters: Filter*) extends Filter with MultiDimensional
