package com.bokland.rubbercube.cube.es

import org.scalatest.{BeforeAndAfterAll, ShouldMatchers, WordSpec}
import com.bokland.rubbercube.cube.{RequestResult, Cube}
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress
import com.bokland.rubbercube.{DateAggregationType, DateAggregation, Dimension}
import com.bokland.rubbercube.measure.Measures.{Sum, CountDistinct}
import com.bokland.rubbercube.measure.DerivedMeasures.Div
import com.bokland.rubbercube.filter.Filter.eql

/**
 * Created by remeniuk on 4/29/14.
 */
class EsExecutionEngineSpec extends WordSpec with ShouldMatchers with BeforeAndAfterAll {

  var engine: EsExecutionEngine = _

  override protected def beforeAll = {
    val settings = ImmutableSettings.settingsBuilder()
      .put("cluster.name", "elasticsearch")
      .put("network.server", true).build()

    val client = new TransportClient(settings)
      .addTransportAddress(new InetSocketTransportAddress("localhost", 9300))

    engine = new EsExecutionEngine(client, "rubbercube")
  }

  "Daily unique payers count" should {
    "be calculated with no filter" in {
      val cube = Cube("purchase",
        Map(Dimension("date") -> DateAggregation(DateAggregationType.Day)),
        Seq(CountDistinct(Dimension("_parent"))))

      engine.execute(cube) should be(
        RequestResult(List(
          Map("date" -> "2014-01-01T00:00:00.000Z", "countdistinct-_parent" -> 2),
          Map("date" -> "2014-01-02T00:00:00.000Z", "countdistinct-_parent" -> 1),
          Map("date" -> "2014-01-03T00:00:00.000Z", "countdistinct-_parent" -> 1)
        )))
    }

    "be calculated with filter, applied to purchase" in {
      val cube = Cube("purchase",
        Map(Dimension("date") -> DateAggregation(DateAggregationType.Day)),
        Seq(CountDistinct(Dimension("_parent"))),
        Seq(eql(Dimension("country"), "US"), eql(Dimension("gender"), "Female"))
      )

      engine.execute(cube) should be(
        RequestResult(List(
          Map("date" -> "2014-01-01T00:00:00.000Z", "countdistinct-_parent" -> 1),
          Map("date" -> "2014-01-02T00:00:00.000Z", "countdistinct-_parent" -> 1)
        ))
      )
    }

    "be calculated with filter, applied to parent document" in {
      val cube = Cube("purchase",
        Map(Dimension("date") -> DateAggregation(DateAggregationType.Day)),
        Seq(CountDistinct(Dimension("_parent"))),
        Seq(eql(Dimension("country"), "US"), eql(Dimension("source", cubeId = Some("user")), "Organic")),
        parentId = Some("user")
      )

      engine.execute(cube) should be(
        RequestResult(List(
          Map("date" -> "2014-01-01T00:00:00.000Z", "countdistinct-_parent" -> 1),
          Map("date" -> "2014-01-02T00:00:00.000Z", "countdistinct-_parent" -> 1)
        ))
      )
    }
  }

  "Revenue per day per daily cohort" in {
    val cube = Cube("purchase",
      Map(Dimension("date") -> DateAggregation(DateAggregationType.Day),
        Dimension("registration_date") -> DateAggregation(DateAggregationType.Day)),
      Seq(Sum(Dimension("amount")), CountDistinct(Dimension("_parent"))))

    engine.execute(cube) should be(
      RequestResult(List(
        Map("date" -> "2014-01-02T00:00:00.000Z", "registration_date" -> "2013-02-01T00:00:00.000Z", "countdistinct-_parent" -> 1, "sum-amount" -> 1.99),
        Map("date" -> "2014-01-01T00:00:00.000Z", "registration_date" -> "2013-01-01T00:00:00.000Z", "countdistinct-_parent" -> 1, "sum-amount" -> 1.99),
        Map("date" -> "2014-01-02T00:00:00.000Z", "registration_date" -> "2013-01-01T00:00:00.000Z", "countdistinct-_parent" -> 1, "sum-amount" -> 4.99),
        Map("date" -> "2014-01-03T00:00:00.000Z", "registration_date" -> "2013-02-01T00:00:00.000Z", "countdistinct-_parent" -> 1, "sum-amount" -> 99.99),
        Map("date" -> "2014-01-01T00:00:00.000Z", "registration_date" -> "2013-02-01T00:00:00.000Z", "countdistinct-_parent" -> 1, "sum-amount" -> 19.99)
      )))
  }

  "Revenue per day" in {
    val cube = Cube("purchase",
      Map(Dimension("date") -> DateAggregation(DateAggregationType.Day)),
      Seq(Sum(Dimension("amount")), CountDistinct(Dimension("_parent"))))

    engine.execute(cube) should be(
      RequestResult(List(
        Map("date" -> "2014-01-01T00:00:00.000Z", "countdistinct-_parent" -> 2, "sum-amount" -> 21.979999999999997),
        Map("date" -> "2014-01-02T00:00:00.000Z", "countdistinct-_parent" -> 1, "sum-amount" -> 6.98),
        Map("date" -> "2014-01-03T00:00:00.000Z", "countdistinct-_parent" -> 1, "sum-amount" -> 99.99)
      )))
  }

  "Revenue per user per day" in {
    val cube = Cube("purchase",
      Map(Dimension("date") -> DateAggregation(DateAggregationType.Day)),
      Seq(Div(Sum(Dimension("amount")), CountDistinct(Dimension("_parent")))))

    engine.execute(cube) should be(
      RequestResult(List(
        Map("date" -> "2014-01-01T00:00:00.000Z", "countdistinct-_parent" -> 2, "sum-amount" -> 21.979999999999997, "div-sum-amount-countdistinct-_parent" -> 10.989999999999998),
        Map("date" -> "2014-01-02T00:00:00.000Z", "countdistinct-_parent" -> 1, "sum-amount" -> 6.98, "div-sum-amount-countdistinct-_parent" -> 6.98),
        Map("date" -> "2014-01-03T00:00:00.000Z", "countdistinct-_parent" -> 1, "sum-amount" -> 99.99, "div-sum-amount-countdistinct-_parent" -> 99.99)
      )))
  }

}
