package org.metaborg.spg.core

/**
  * Language-dependent generation configuration.
  *
  * @param semanticsPath Path to the NaBL2 static semantics file.
  * @param limit Maximum number of terms to generate.
  * @param fuel Fuel provided to the backtracker.
  * @param sizeLimit Maximum size of a term.
  * @param consistency Whether or not to perform the consistency check.
  * @param throwOnUnresolvable Whether or not to throw an exception when a reference can never be resolved.
  */
case class Config(semanticsPath: String, limit: Int, fuel: Int, sizeLimit: Int, consistency: Boolean, throwOnUnresolvable: Boolean)