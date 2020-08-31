import java.io.File
import java.nio.file.Paths
import java.util.concurrent.{Executors, TimeUnit}

import SampleData._
import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.Attributes.LogLevels
import akka.stream.scaladsl.Tcp.{IncomingConnection, ServerBinding}
import akka.stream.scaladsl.{Balance, Broadcast, FileIO, Flow, Framing, GraphDSL, Keep, Merge, Partition, RunnableGraph, Sink, Source, Tcp}
import akka.stream.{ActorAttributes, Attributes, ClosedShape, Supervision}
import akka.util.ByteString
import org.slf4j.LoggerFactory

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

object AkkaStream extends App {
  //Source where everything start
  // It has a output
  implicit val system: ActorSystem = ActorSystem("AkkaStream")
//  implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(system))
  implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(16))

  val source: Source[Account, NotUsed] = Source(sampleAccounts)
  val logger = LoggerFactory.getLogger(getClass.getSimpleName)

  // Flow is where data transformation
  //It has an input and an output
  val accountFlow: Flow[Account, List[Campaign], NotUsed] = Flow[Account].map(account => {
    logger.info(s"Insert account ${account.name}")
    fakeInsert(s"Insert account ${account.name}")
    val campaignIn = for (campaign <- sampleCampaigns; if campaign.accountId == account.id) yield campaign
    campaignIn
  })

  val campaignFlow: Flow[Campaign, List[Keyword], NotUsed] = Flow[Campaign].map(campaign => {
    logger.info(s"Insert campaign ${campaign.name}")
    fakeInsert(s"Insert campaign ${campaign.name}")
    val keywordsIn = for (keyword <- sampleKeywords; if keyword.campaignId == campaign.id) yield keyword
    keywordsIn
  })

  val keywordFlow: Flow[Keyword, Unit, NotUsed] = Flow[Keyword].map(keyword => {
    logger.info(s"Insert keyword ${keyword.name}")
    fakeInsert(s"Insert keyword ${keyword.name}")
  })

  //val start = System.nanoTime()
  ////  source via accountFlow mapConcat identity via campaignFlow mapConcat identity via keywordFlow runWith Sink.ignore onComplete{
  ////    case Success(_) =>
  ////      println("Stream completed successfully")
  ////      system.terminate()
  ////      val end = System.nanoTime()
  ////      println(s"Time taken: ${(end - start) / 1000 / 1000/ 1000} s")
  ////    case Failure(error) =>
  ////      println(s"Stream failed with error ${error.getMessage}")
  ////      system.terminate()
  ////  }

  def currentSecond = System.currentTimeMillis() / 1000
  def f1(x: Int) = {
    println(s"f1::begin $x")
    logger.info(s"info for f1::begin $x")
    val start = currentSecond
    while (currentSecond - start <= 2) {}
    x * 2
  }

  def f2(x: Int) = {
    println(s"f2::begin $x")
    logger.info(s"info for f2::begin $x")
    val start = currentSecond
    while (currentSecond - start <= 5){}
    x * 2
  }
  val start = currentSecond
  Source(List(1,3,5,7,9))
    //.mapAsyncUnordered(6)(x => Future(f1(x))).async
    //.mapAsyncUnordered(6)(x => Future(f2(x))).async
    .map(f1).async
    .map(f2)
    //.mapAsyncUnordered(6)(x => Future(f1(x)))
    //.mapAsyncUnordered(6)(x => Future(f2(x)))
    //.map(f1).async
    //.map(f2).async
    .runWith(Sink.foreach[Int](x => {
      logger.info(s"info for sink $x")
      println(s"sink $x")}))
    .onComplete { case x => println(s"compute time: ${currentSecond - start}") }
}

object AkkaStream_1 extends App {
  implicit val system = ActorSystem("reactive-tweets")
  implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(16))

  val source = Source(1 to 10)

  val flow = Flow[Int].map(_ * 2)
  val sink = Sink.fold[Int, Int](0)( _ + _ )
  //val sink: Sink[Int, Future[Done]] = Sink.foreach(println)

  val runnable_ne: RunnableGraph[Future[Int]]=
    source
      .via(flow)
      .log("AkkaStream_1")
      .withAttributes(Attributes
        .logLevels(onElement = Logging.InfoLevel, onFinish = Logging.InfoLevel, onFailure = Logging.ErrorLevel))
      .toMat(sink)(Keep.right)

  val a = runnable_ne.run()


  val source_2 = Source.single(ByteString("abcxyz"))
  val runnable_2 = source_2.log("AkkaStream_2")
    .withAttributes(Attributes
      .logLevels(onElement = Logging.InfoLevel, onFinish = Logging.InfoLevel, onFailure = Logging.ErrorLevel))to(Sink.foreach(println))


source_2.run()
}

object AkkaCombineSource extends App {
  import akka.stream.scaladsl.Source

  implicit val system = ActorSystem("reactive-tweets")
  val s1 = Source(List(1,3,5,7))
  val s2 = Source(List(2,4,6,8))

  val multiplyFlow = Flow[Int].throttle(5, new FiniteDuration(1, TimeUnit.SECONDS)).map( _ * 2)
  val toStringFLow = Flow[Int].map(_.toString)

  val runnable = Source.combine(s1, s2)(Merge(_))
    .via(multiplyFlow)
    .via(toStringFLow)
    .runWith(Sink.foreach(println))

}

object AkkaGroupedSource extends App {
  implicit val system = ActorSystem("reactive-tweets1")
  val source = Source(1 to 100)

  val flow = Flow[Int].grouped(10).throttle(1, new FiniteDuration(5, TimeUnit.SECONDS)).map( x => x.map(_ *2))

  val runnable_4 = source.via(flow).to(Sink.foreach(println))

  runnable_4.run()
}

object MapASync extends App {
  implicit val system = ActorSystem("reactive-tweets12")
  implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(16))

  case class User(id:Int, name: String) {
    override def toString: String = name
  }

  def querySync(id: Int): User = {
    println(s"start query - $id")
    Thread.sleep(1000)
    println(s"finish query - $id")
    User(id, "user" + id)
  }

  val source = Source(1 to 5)
  val queryFlow = Flow[Int].map(querySync)
  val queryFlowAsync = Flow[Int].mapAsync(5)(x => Future(querySync(x)))

  //graph
  source
    //.via(queryFlow)
    .via(queryFlowAsync)
    .runWith(Sink.foreach(println))
}

object Broadcast_1 extends App {
  case class Idol(id: Int, name: String, role: String)
  implicit val system = ActorSystem("reactive-tweets12")
  val logger = LoggerFactory.getLogger(getClass.getSimpleName)
  val idols: List[Idol] = List(Idol(1, "JungKook", "Visual"), Idol(2, "Jimin", "Lead Vocal"), Idol(3, "Sugar", "Rapper"),Idol(4, "RM", "Producer") )
  val source = Source(idols)
  val idFlow = Flow[Idol].map(_.id)
  val nameFlow = Flow[Idol].map(_.name)
  val roleFlow = Flow[Idol].map(_.role)

  val idSink = Sink.foreach[Int](p => {
    logger.info(s"Id - $p")
    println(s"Id - $p")
  })
  val nameSink = Sink.foreach[String](p => {
    logger.info(s"Name - $p")
    println(s"Name - $p")
  })
  val roleSink = Sink.foreach[String](p => {
    logger.info(s"Role - $p")
    println(s"Role - $p")
  })

  val graph = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._
    // broadcaster to broadcast list elements[(String, Int)] with two streams
    val broadcaster = b.add(Broadcast[Idol](3))
    // source directs to broadcaster
    source ~> broadcaster.in
    // broadcast list element with two streams
    //  1. to name sink
    //  2. to rate sink
    broadcaster.out(0) ~> idFlow ~> idSink
    broadcaster.out(2) ~> nameFlow ~> nameSink
    broadcaster.out(1) ~> roleFlow ~> roleSink

    ClosedShape
  })
  graph.run()
}

object Balance_1 extends App {
  case class Idol(id: Int, name: String, role: String)
  implicit val system = ActorSystem("reactive-tweets12")
  val logger = LoggerFactory.getLogger(getClass.getSimpleName)
  val idols: List[Idol] = List(Idol(1, "JungKook", "Visual"), Idol(2, "Jimin", "Lead Vocal"), Idol(3, "Sugar", "Rapper"),Idol(4, "RM", "Producer"),  Idol(5, "V", "Visual_2"), Idol(6, "Jin", "Mother Fucker"))
  val source = Source(idols)

  val backPressureFlow = Flow[Idol].throttle(1,new FiniteDuration(5, TimeUnit.SECONDS))
  val normalFlow = Flow[Idol]

  val backPressureSink = Sink.foreach[Idol](p => println(s"Coming from back-pressured flow - $p.toString()"))
  val normalSink = Sink.foreach[Idol](p => println(s"Coming from normal flow - $p.toString()"))

  val graph = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._
    val balancer = b.add(Balance[Idol](2))

    source ~> balancer

    balancer.out(0) ~> backPressureFlow ~> backPressureSink
    balancer.out(1) ~> normalFlow ~> normalSink
    ClosedShape
  }).run()
}

object GroupBy extends App {
  implicit val system = ActorSystem("reactive-tweets12")
  val logger = LoggerFactory.getLogger(getClass.getSimpleName)
  Source(1 to 10)
    .groupBy(4, _ % 3)
    .map(x => {
     logger.info(s"info $x")
      x
    }).async
    .mergeSubstreams
    .runWith(Sink.foreach(println))

}

object Partition_1 extends App {

  case class Arg(name: String)

  case class Result(name: String, rating: Int)

  implicit val system = ActorSystem("reactive-tweets12")
  val logger = LoggerFactory.getLogger(getClass.getSimpleName)


  val resp = List(
    (Arg("scala"), Option(Result("scala", 4))),
    (Arg("golang"), Option(Result("golang", 6))),
    (Arg("java"), None),
    (Arg("haskell"), Option(Result("haskell", 5))),
    (Arg("erlang"), Option(Result("erlang", 5)))
  )
  val source = Source(resp)

  val ratingFlow = Flow[(Arg, Option[Result])].map(_._2)
  val argFlow = Flow[(Arg, Option[Result])].map(_._1)

  // sinks to handle
  //  1. when having result, handle Rating
  //  2. when None result, handle Arg
  val ratingSink = Sink.foreach[Option[Result]](r => {
    logger.info(s"Rating found, name - ${r.get.name}, rating - ${r.get.rating}")
    println(s"Rating found, name - ${r.get.name}, rating - ${r.get.rating}")
  })
  val argSink = Sink.foreach[Arg](r => {
    logger.info(s"Rating not found, arg - ${r.name}")
    println(s"Rating not found, arg - ${r.name}")
  })

  val graph = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._
    val partition = b.add(Partition[(Arg, Option[Result])](2, x => if (x._2.isDefined) 0 else 1))

    source ~> partition.in
    // flow stream in two flows
    //  1. with rating
    //  2. without rating
    partition.out(0) ~> ratingFlow ~> ratingSink
    partition.out(1) ~> argFlow ~> argSink
    ClosedShape
  }).run()
}

object Partition_2 extends  App {
  implicit val system = ActorSystem("reactive-tweets12")
  val source: Source[Int, NotUsed] = Source(1 to 10)

  val even: Sink[Int, NotUsed] =
    Flow[Int].log("even").withAttributes(Attributes.logLevels(onElement = LogLevels.Info)).to(Sink.ignore)
  val odd: Sink[Int, NotUsed] =
    Flow[Int].log("odd").withAttributes(Attributes.logLevels(onElement = LogLevels.Info)).to(Sink.ignore)

  RunnableGraph
    .fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      val partition = builder.add(Partition[Int](2, element => if (element % 2 == 0) 0 else 1))
      source ~> partition.in
      partition.out(0) ~> even
      partition.out(1) ~> odd
      ClosedShape
    })
    .run()
}

object MapConcat extends App {
  def duplicate(i: Int): List[Int] = List(i, i)
  implicit val system = ActorSystem("reactive-tweets12")

  Source(1 to 3).mapConcat(duplicate).runForeach(println)
}

object TestLog extends App {
  Source(-5 to 5)
    .map( 1 / _ )
    .log("test log")
    .withAttributes(Attributes.logLevels())
}

object TestRecover extends App {
  implicit val system = ActorSystem("reactive-tweets12")
  Source(0 to 6)
    .map(
      n =>
        // assuming `4` and `5` are unexpected values that could throw exception
        if (List(4, 5).contains(n)) throw new RuntimeException(s"Boom! Bad value found: $n")
        else n.toString)
    .recover {
      case e: OutOfMemoryError => e.getMessage
    }
    .runForeach(println)
}

object TestRecoverWith extends App {
  implicit val system = ActorSystem("reactive-tweets12")

  val planB = Source(List("five", "six", "seven", "eight"))
  Source(0 to 10)
    .map(n =>
      if (n < 5) n.toString
      else throw new RuntimeException("Boom!"))
    .recoverWithRetries(attempts = 5, {
      case _: RuntimeException => {
        println("a")
        planB
      }
    })
    .runForeach(println)
}

object DelayedRestart extends App {
  implicit val system = ActorSystem("reactive-tweets12")

//  val restartSource = RestartSource.withBackoff(
//    maxBackoff = Duration.ofSeconds(3),
//    minBackoff = Duration.ofSeconds(30),
//    randomFactor = 0.5,
//    maxRestarts = 20,
//    sourceFactory = {
//      Source[ServerSentEvent]
//    }
////  )
//val restartSource = RestartSource.withBackoff(
//  minBackoff = Duration.ofSeconds(3),
//  maxBackoff = Duration.ofSeconds(30),
//  randomFactor = 0.2, // adds 20% "noise" to vary the intervals slightly
//  maxRestarts = 20 // limits the amount of restarts to 20
//) { () =>
//  // Create a source from a future of a source
//  Source.fromFutureSource {
//    // Make a single request with akka-http
//    Http()
//      .singleRequest(HttpRequest(uri = "http://example.com/eventstream"))
//      // Unmarshall it as a source of server sent events
//      .flatMap(Unmarshal(_).to[Source[ServerSentEvent, NotUsed]])
//  }
//}
}

object SupervisionStrategies extends App {
  implicit val system = ActorSystem("reactive-tweets12")

  val decider: Supervision.Decider = {
    case _ : ArithmeticException => Supervision.Restart
    case _ => Supervision.Stop
  }

  val runnableGraph = Source(-5 to 5).map(100 / _).toMat(Sink.foreach(println))(Keep.right)
  val withCustomSupervision = runnableGraph.withAttributes(ActorAttributes.supervisionStrategy(decider))

  val result = withCustomSupervision.run()


}

object Restart extends App {
  implicit val system = ActorSystem("reactive-tweets12")

  val decider: Supervision.Decider = {
    case _: IllegalArgumentException => Supervision.Restart
    case _                           => Supervision.Stop
  }
  val flow = Flow[Int]
    .scan(0) { (acc, elem) =>
      println( "acc: " + acc + " elem: " + elem)
      if (elem < 0) throw new IllegalArgumentException("negative not allowed")
      else acc + elem
    }
    .withAttributes(ActorAttributes.supervisionStrategy(decider))
  val source = Source(List(1, 3, -1, 5, 7)).via(flow)
  val result = source.limit(1000).runWith(Sink.foreach(println))
}

object IOTest1 extends App {
  implicit val system = ActorSystem("reactive-tweets12")

  val host = "127.0.0.1"
  val port = 8080
  val connections: Source[IncomingConnection, Future[ServerBinding]] =
    Tcp().bind(host, port)
  connections.runForeach { connection =>
    println(s"New connection from: ${connection.remoteAddress}")

    val echo = Flow[ByteString]
      .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 256, allowTruncation = true))
      .map(_.utf8String)
      .map(_ + "!!!\n")
      .map(ByteString(_))

    connection.handleWith(echo)
  }
}

object IOReadFile extends App {
  implicit val system = ActorSystem("reactive-tweets12")
//val logFile = new File("src/logFile.txt")

  val file = Paths.get("/Users/duc_tv/Desktop/P4T/test_akka_family/src/main/scala/logFile")

  val source = FileIO.fromPath(file)

  // parse  chucks of bytes into line
  val flow = Framing.delimiter(ByteString(System.lineSeparator()), maximumFrameLength = 512, allowTruncation = true).map(_.utf8String)

  val sink = Sink.foreach(println)

  source.via(flow).runWith(sink)
}

object IOWriteFIle extends App {
  implicit val system = ActorSystem("reactive-tweets12")

  val source = Source(1 to 10000)

  val outputPath = Paths.get("/Users/duc_tv/Desktop/P4T/test_akka_family/src/main/scala/logFile_1")

  val fileSink = FileIO.toPath(outputPath)

  val consoleSink = Sink.foreach(println)

  val flow = Flow[Int].map(num => ByteString(num.toString))


  val graph = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._
    // broadcaster to broadcast list elements[(String, Int)] with two streams
    val broadcaster = b.add(Broadcast[Int](2))
    // source directs to broadcaster
    source ~> broadcaster.in
    // broadcast list element with two streams
    //  1. to name sink
    //  2. to rate sink
    broadcaster.out(0) ~> flow ~> fileSink
    broadcaster.out(1) ~> consoleSink

    ClosedShape
  }).run()
}
