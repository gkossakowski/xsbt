logLevel := Level.Debug

compile in Compile <<= (compile in Compile, savedAnalysis) map { (a: sbt.inc.Analysis, buf: scala.collection.mutable.Buffer[sbt.inc.Analysis]) =>
  buf += a
  a
}

TaskKey[Unit]("check-names") <<= (savedAnalysis, baseDirectory) map { (ab: scala.collection.mutable.Buffer[sbt.inc.Analysis], base: java.io.File) =>
  val (a1: sbt.inc.Analysis, a2: sbt.inc.Analysis) = {
    val last2 = ab.takeRight(2)
    (last2(0), last2(1))
  }
  def sameNameHashes(nms1: Set[xsbti.api.NameHash], nms2: Set[xsbti.api.NameHash]): Boolean = {
    // we have to convert NameHash instances to pairs because they do not implement proper equals()
    val pairs1 = nms1.map(x => x.name -> x.hash)
    val pairs2 = nms2.map(x => x.name -> x.hash)
    pairs1 == pairs2
  }
  // test src/main/scala/B.scala, name hashes should be equal
  {
    val file = base / "src/main/scala/B.scala"
    assert(a1.apis.internal.keySet.contains(file))
    val nms1 = a1.apis.internalAPI(file).nameHashes.toSet
    val nms2 = a2.apis.internalAPI(file).nameHashes.toSet
    assert(sameNameHashes(nms1, nms2), "hashes are not the same in " + file + " nms1 = " + nms1 + " nms2 = " + nms2)
  }
  // test src/main/scala/A.scala, name should be equal except for one corresponding to "bar"
  {
    val file = base / "src/main/scala/A.scala"
    assert(a1.apis.internal.keySet.contains(file))
    val nms1 = a1.apis.internalAPI(file).nameHashes.toSet
    val nms2 = a2.apis.internalAPI(file).nameHashes.toSet
    assert(sameNameHashes(nms1.filterNot(_.name == "bar"), nms2.filterNot(_.name == "bar")), "hashes are not the same in " + file + " nms1 = " + nms1 + " nms2 = " + nms2)
    assert(nms2.exists(_.name == "bar"), """hash sum for "bar" does not exist""")
  }
}

InputKey[Unit]("check-number-of-compiler-iterations") <<= inputTask { (argTask: TaskKey[Seq[String]]) =>
  (argTask, compile in Compile) map { (args: Seq[String], a: sbt.inc.Analysis) =>
    assert(args.size == 1)
    val expectedIterationsNumber = args(0).toInt
    assert(a.compilations.allCompilations.size == expectedIterationsNumber, "a.compilations.allCompilations.size = %d (expected %d)".format(a.compilations.allCompilations.size, expectedIterationsNumber))
  }
}