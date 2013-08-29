package sbt.inc

import java.io.File
import sbt.Logger
import sbt.Relation

class NameHashFilteredDependencies2(
		names: Relation[File, String],
		reversedMemberRefDeps: File => Set[File],
		modifiedNames: Set[String],
		log: Logger) extends (File => Set[File]) {

	private val cachedResults: collection.mutable.Map[File, Set[File]] = collection.mutable.Map.empty

	def apply(to: File): Set[File] = {
		val dependent = reversedMemberRefDeps(to)
		cachedResults.getOrElseUpdate(to, filteredDependencies(dependent))
	}

	private def filteredDependencies(dependent: Set[File]) = {
		dependent.filter {
			case from if fileExtension(from) == "java" =>
				log.debug(s"Name hashing optimization doesn't apply to Java dependency: $from")
				true
			case from =>
				val usedNamesInDependent = usedNames(from)
				val modifiedAndUsedNames = modifiedNames intersect usedNamesInDependent
				if (modifiedAndUsedNames.isEmpty) {
					log.debug("None of the modified names appears in %s. This dependency is not being considered for invalidation.".format(from))
					false
				} else {
					log.debug("The following modified names cause invalidation of %s: %s".format(from, modifiedAndUsedNames))
					true
				}
		}
	}

	private def usedNames(from: File): Set[String] = names.forward(from)

	/** Returns file name extenstion or empty string if passed file doesn't have an extenstion */
	private def fileExtension(file: File): String = {
		val name = file.getName
		val lastDotIndex = name.lastIndexOf('.')
		val extension = if (lastDotIndex == -1) "" else name.substring(lastDotIndex+1)
		extension
	}

}