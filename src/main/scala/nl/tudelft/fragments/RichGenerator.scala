package nl.tudelft.fragments

import nl.tudelft.fragments.spoofax.Language
import rx.lang.scala.Observable

import scala.collection.mutable

/**
  * A generator that collects statistics on the terms it generates.
  */
class RichGenerator {
  /**
    * Returns a cold observable that emits the running averages.
    *
    * @param projectPath
    * @param semanticsPath
    * @param limit
    * @param interactive
    * @param verbose
    * @return
    */
  def generate(projectPath: String, semanticsPath: String, config: Config, limit: Int, interactive: Boolean, verbose: Boolean): Observable[mutable.Map[String, Int]] =
    generate(Language.load(projectPath, semanticsPath), config, limit, interactive, verbose)

  def generate(language: Language, config: Config, limit: Int, interactive: Boolean, verbose: Boolean): Observable[mutable.Map[String, Int]] = {
    val statistics = mutable.Map[String, Int](
      language.constructors.map(cons => (cons, 0)): _*
    )

    new Generator().generate(language, config, limit, interactive, verbose).map(result => {
      // Constructors
      val appls = result.rule.pattern.collect {
        case t@TermAppl(_, _) =>
          List(t)
        case _ =>
          Nil
      }

      val constructors = appls.map {
        case TermAppl(cons, _) =>
          cons
      }

      val groups = constructors.group.toList.map {
        case (cons, conss) =>
          (cons, conss.length)
      }

      for ((cons, length) <- groups) {
        statistics.update(cons, statistics(cons) + length)
      }

      /*
      // Distribution of terms
      println("## Distribution of terms")

      val grouped = states.groupByWith(_.pattern, Pattern.equivalence).toList
        // Bind the size of the group
        .map {
        case (pattern, states) =>
          (states.size, pattern, states)
      }
        // Sort descending by size
        .sortBy {
        case (size, pattern, states) =>
          -size
      }

      grouped.foreach { case (size, pattern, states) =>
        println(s"${size}x $pattern")
      }

      println(s"${grouped.size} groups")
      */

      /*
      // General
      // Term size
      val termSizes = states.map(state => state.pattern.size)

      // Number of name resolutions
      val resolutionCounts = states.map(state => state.resolution.size)

      // Count constructor occurrences
      println("# Statistics")

      println("Generated terms = " + states.size)
      println("Average term size = " + termSizes.average)
      println("Average resolution count = " + resolutionCounts.average)
      */

      statistics
    })
  }
}

object RichGenerator {
  /**
    * Entry point when running as CLI. Takes a path to a Spoofax project as
    * argument and outputs running statistics.
    *
    * @param args
    */
  def main(args: Array[String]): Unit = {
    if (args.length == 0) {
      println("Usage: Generator <path> [options]")
      println("  --limit <n>   Generate at most n terms")
      println("  --interactive Interactive mode")
      println("  --verbose     Verbose output")
    } else {
      def parseOptions(options: List[String], config: Map[String, String] = Map.empty): Map[String, String] = options match {
        case "--semantics" :: semantics :: rest =>
          parseOptions(rest, config + ("semantics" -> semantics))
        case "--limit" :: n :: rest =>
          parseOptions(rest, config + ("limit" -> n))
        case "--interactive" :: n :: rest =>
          parseOptions(rest, config + ("interactive" -> n))
        case "--verbose" :: rest =>
          parseOptions(rest, config + ("verbose" -> "true"))
        case Nil =>
          config
        case _ :: rest =>
          parseOptions(rest, config)
      }

      val options = parseOptions(args(1).split(' ').toList)
      val projectPath = args(0)
      val semanticsPath = options.get("semantics").map(_.toString).getOrElse("trans/static-semantics.nabl2")
      val limit = options.get("limit").map(_.toInt).getOrElse(-1)
      val interactive = options.get("interactive").map(_.toBoolean).getOrElse(false)
      val verbose = options.get("verbose").map(_.toBoolean).getOrElse(false)

      new RichGenerator().generate(projectPath, semanticsPath, DefaultConfig, limit, interactive, verbose).subscribe(stats => {
        for ((k, v) <- stats) {
          println(s"$k = $v")
        }
      })
    }
  }
}