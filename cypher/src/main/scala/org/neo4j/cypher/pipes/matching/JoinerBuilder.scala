/**
 * Copyright (c) 2002-2011 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.pipes.matching

import org.neo4j.graphdb.Direction
import org.neo4j.cypher.symbols.RelationshipType
import org.neo4j.cypher.commands.{True, And, Clause}

object JoinerBuilder {
  def canHandlePatter(patternGraph: PatternGraph): Boolean = false &&
    !(patternGraph.containsOptionalElements ||
      patternGraph.containsLoops ||
      patternGraph.bindings.identifiers.exists(_.typ == RelationshipType()) ||
      patternGraph.patternRels.values.exists(_.isInstanceOf[VariableLengthPatternRelationship])
      )
}

class JoinerBuilder(patternGraph: PatternGraph, clauses: Seq[Clause]) extends MatcherBuilder {
  val joiner = createAndLinkJoiners(patternGraph.boundElements.map(patternGraph.patternNodes(_)))

  def getMatches(sourceRow: Map[String, Any]): Traversable[Map[String, Any]] = {
    joiner.getResult(sourceRow)
  }

  def createAndLinkJoiners(alreadyCovered: Seq[PatternNode]): Linkable = {
    var joiner: Linkable = new Start(alreadyCovered.map(_.key))
    var done = alreadyCovered
    val clauseHolder = new ClauseHolder(clauses)

    while (done.size < patternGraph.patternNodes.size) {

      val patternRelationshipsNotAlreadyDone = done.
        flatMap(n1 => n1.relationships.filterNot(r => done.contains(r.getOtherNode(n1))))

      patternRelationshipsNotAlreadyDone.foreach(rel => {
        val (start, end, dir) =
          if (done.contains(rel.startNode)) {
            done = done ++ Seq(rel.endNode)
            (rel.startNode.key, rel.endNode.key, figureOutDirection(rel.dir, true))
          } else {
            done = done ++ Seq(rel.startNode)
            (rel.endNode.key, rel.startNode.key, figureOutDirection(rel.dir, false))
          }

        val doneKeys = joiner.providesKeys() ++ Seq(rel.key, end)

        val clause = clauseHolder.getMatchingClauses(doneKeys)

        joiner = new Joiner(joiner, start, dir, end, rel.relType, rel.key, clause)
      })

    }

    joiner
  }

  private def figureOutDirection(dir: Direction, reverse: Boolean) = dir match {
    case Direction.BOTH => Direction.BOTH
    case Direction.OUTGOING => if (reverse) Direction.OUTGOING else Direction.INCOMING
    case Direction.INCOMING => if (reverse) Direction.INCOMING else Direction.OUTGOING
  }
}

class ClauseHolder(var clauses: Seq[Clause]) {
  def getMatchingClauses(keys: Seq[String]) = {

    val (newClauses, matchingClauses) = clauses.partition(x => x.dependsOn.filterNot(keys.contains).isEmpty)
    clauses = newClauses

    val clause = if (matchingClauses.isEmpty)
      True()
    else
      matchingClauses.reduceLeft(And(_, _))

    clause
  }
}