package nl.tudelft.fragments

import org.scalatest.FunSuite

class LabelSuite extends FunSuite {
  val labels = List(
    Label('D'),
    Label('P'),
    Label('I')
  )

  val ordering = LabelOrdering(
    (Label('D'), Label('P')),
    (Label('D'), Label('I')),
    (Label('I'), Label('P'))
  )

  test("max on label ordering") {
    assert(LabelOrdering.max(labels, ordering) == List(Label('P')))
  }

  test("lt on label ordering") {
    assert(LabelOrdering.lt(labels, Label('I'), ordering) == List(Label('D')))
  }
}