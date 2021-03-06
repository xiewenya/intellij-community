// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.actionSystem

import com.intellij.diagnostic.PluginException
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.openapi.extensions.PluginDescriptor
import com.intellij.openapi.extensions.PluginId
import com.intellij.util.SmartFMap
import java.util.*
import java.util.function.Supplier

interface ActionStubBase {
  val id: String

  @Deprecated(message = "Use plugin", replaceWith = ReplaceWith("plugin.pluginId"))
  @JvmDefault
  val pluginId: PluginId?
    get() = plugin.pluginId

  val plugin: PluginDescriptor
  val iconPath: String?
}

class ActionGroupStub(override val id: String, val actionClass: String, override val plugin: IdeaPluginDescriptor) : DefaultActionGroup(), ActionStubBase {
  val classLoader: ClassLoader
    get() = plugin.pluginClassLoader

  var popupDefinedInXml = false

  override var iconPath: String? = null

  private var myActionTextOverrides = SmartFMap.emptyMap<String, Supplier<String>>()

  override fun addTextOverride(place: String, text: Supplier<String>) {
    myActionTextOverrides = myActionTextOverrides.plus(place, text)
  }

  fun initGroup(target: ActionGroup, actionManager: ActionManager) {
    ActionStub.copyTemplatePresentation(templatePresentation, target.templatePresentation)
    for ((place, text) in myActionTextOverrides) {
      target.addTextOverride(place, text)
    }

    target.shortcutSet = shortcutSet
    val children = getChildren(null, actionManager)
    if (children.isNotEmpty()) {
      target as? DefaultActionGroup
      ?: throw PluginException("Action group class must extend DefaultActionGroup for the group to accept children: $actionClass", plugin.pluginId)
      for (action in children) {
        target.addAction(action, Constraints.LAST, actionManager)
      }
    }
    if (popupDefinedInXml) {
      target.isPopup = isPopup
    }
    target.isSearchable = isSearchable
  }
}
