/*
 * Copyright 2014 Rik van der Kleij
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.powertuple.intellij.haskell.external

import com.intellij.openapi.project.Project
import com.powertuple.intellij.haskell.settings.HaskellSettings

object GhcMod {

  import scala.collection.JavaConversions._

  def browseInfo(project: Project, moduleNames: Seq[String], removeParensFromOperator: Boolean): Seq[BrowseInfo] = {
    val output = ExternalProcess.getProcessOutput(
      project.getBasePath,
      HaskellSettings.getInstance().getState.ghcModPath,
      Seq("browse", "-d", "-q", "-o") ++ moduleNames
    )
    val browseInfos = output.getStdoutLines.map(_.split("::")).map(cols => {
      val declaration = if (cols.size == 2) cols(1).trim else ""
      val qualifiedName = cols(0)

      val indexOfOperator = qualifiedName.lastIndexOf(".(") + 1
      val (module, name) = if (indexOfOperator > 1) {
        val (m, o) = trimPair(qualifiedName.splitAt(indexOfOperator))
        (m.substring(0, m.length - 1), if (removeParensFromOperator) o.substring(1, o.length - 1) else o)
      } else {
        val indexOfId = qualifiedName.lastIndexOf('.') + 1
        val (m, id) = trimPair(qualifiedName.splitAt(indexOfId))
        (m.substring(0, m.length - 1), id)
      }
      BrowseInfo(name, module, declaration)
    })
    browseInfos
  }

  private def trimPair(t: (String, String)) = {
    t match {
      case (t0, t1) => (t0.trim, t1.trim)
    }
  }

  def listAvailableModules(project: Project): Seq[String] = {
    val output = ExternalProcess.getProcessOutput(
      project.getBasePath,
      HaskellSettings.getInstance().getState.ghcModPath,
      Seq("list")
    )
    output.getStdoutLines
  }

  def check(project: Project, filePath: String): GhcModiOutput = {
    val output = ExternalProcess.getProcessOutput(
      project.getBasePath,
      HaskellSettings.getInstance().getState.ghcModPath,
      Seq("check", filePath)
    )
    GhcModiOutput(output.getStdoutLines)
  }
}

case class BrowseInfo(name: String, moduleName: String, declaration: String)