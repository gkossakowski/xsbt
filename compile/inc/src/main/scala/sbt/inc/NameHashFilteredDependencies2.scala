package sbt.inc

import java.io.File
import sbt.Logger
import sbt.Relation

class NameHashFilteredDependencies2(
		names: File => Set[String],
		reversedMemberRefDeps: String => Set[File],
		namesWithModifiedHashesInSource: NamesWithModifiedHashesInSource,
		log: Logger) extends (String => Set[File]) {

	private val cachedResults: collection.mutable.Map[String, Set[File]] = collection.mutable.Map.empty
	private val modifiedNamesInClass = {
		val inClass = namesWithModifiedHashesInSource.namesWithModifiedHashesInClass
		def emptyModifiedNames(className: String) = NamesWithModifiedHashesInClass(className, Set.empty, Set.empty)
		val modifiedNamesMap = inClass.map(x => x.className -> x).toMap
		modifiedNamesMap.withDefault(emptyModifiedNames)
	}

	def apply(to: String): Set[File] = {
		val dependent = reversedMemberRefDeps(to)
		val modifiedNames = modifiedNamesInClass(to).regularNames
		cachedResults.getOrElseUpdate(to, filteredDependencies(modifiedNames, dependent))
	}

	private def filteredDependencies(modifiedNames: Set[String], dependent: Set[File]) = {
		dependent.filter {
			case from if isJavaSource(from) =>
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

	private def usedNames(from: File): Set[String] = names(from)

	def isJavaSource(file: File): Boolean = fileExtension(file) == "java"

	/** Returns file name extenstion or empty string if passed file doesn't have an extenstion */
	private def fileExtension(file: File): String = {
		val name = file.getName
		val lastDotIndex = name.lastIndexOf('.')
		val extension = if (lastDotIndex == -1) "" else name.substring(lastDotIndex+1)
		extension
	}

}
