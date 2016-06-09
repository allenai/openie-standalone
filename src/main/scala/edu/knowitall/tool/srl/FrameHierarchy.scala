package edu.knowitall.tool.srl

import edu.knowitall.tool.parse.graph.DependencyGraph
import edu.knowitall.collection.immutable.graph.Direction

case class FrameHierarchy(frame: Frame, children: Seq[FrameHierarchy]) {
  def height: Int =
    if (children.isEmpty) {
      1
    }
    else {
      1 + children.iterator.map(_.height).max
    }
  override def toString = frame.toString + (if (children.size > 0) (" < " + children.mkString(", ")) else "")
}

object FrameHierarchy {
  def fromFrames(dgraph: DependencyGraph, frames: Seq[Frame]) = {
    if (frames.isEmpty) {
      Seq.empty
    }
    else {
      val framesWithIndex = frames.zipWithIndex

      // find all ancestor -> descendant relationships
      val descendants = framesWithIndex.map {
        case (frame, index) =>
          val inferiors = dgraph.graph.inferiors(frame.relation.node, dedge => !(dedge.source == frame.relation.node && dedge.label == "conj"))
          val children = framesWithIndex.filter {
            case (child, index) =>
              frame != child && (
                // first arguments must match
                (for (frameRole <- frame.argument(Roles.A0); childRole <- child.argument(Roles.A0)) yield (frameRole == childRole)).getOrElse(false) &&
                // child frame must be beneath parent frame relation in dependency graph
                (inferiors contains child.relation.node) ||
                // all child nodes are in the inferiors of the relation
                (child.nodes.forall(inferiors contains _)) &&
                // relations are connected by ccomp
                (dgraph.graph.neighbors(
                  frame.relation.node,
                  dedge => dedge.dir == Direction.Down && dedge.edge.label == "ccomp"
                ).contains(child.relation.node))
              )
          }
          index -> children.map(_._2).toSet
      }.toMap

      def transitiveClosure(hierarchy: Map[Int, Set[Int]]): Map[Int, Set[Int]] = {
        val targets = hierarchy.map {
          case (parent, children) =>
            val descendants = children flatMap hierarchy
            // add children of children
            if (descendants != children) {
              parent -> (descendants ++ children)
            }
            // there is no change
            else {
              parent -> children
            }
        }

        // unstable, continue
        if (targets != hierarchy) {
          transitiveClosure(targets)
        }
        // stable, stop
        else {
          targets
        }
      }

      val closure = transitiveClosure(descendants)
      var hierarchies = Map.empty[Int, FrameHierarchy]
      for (i <- Range(0, closure.iterator.map(_._2.size).max + 1)) {
        val targetDescendants = closure.filter(_._2.size == i)

        for ((frameIndex, childrenIndices) <- targetDescendants) {
          val frame = frames(frameIndex)
          val children = childrenIndices flatMap hierarchies.get
          hierarchies --= childrenIndices

          hierarchies += frameIndex -> FrameHierarchy(frame, children.toSeq)
        }
      }

      hierarchies.map(_._2)
    }
  }
}
