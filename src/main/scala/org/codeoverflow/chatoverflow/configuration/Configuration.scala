package org.codeoverflow.chatoverflow.configuration

import scala.xml.{Elem, Node}

trait Configuration {
  def toXml: xml.Elem
}

case class PluginInstance(pluginName: String, pluginAuthor: String, instanceName: String) extends Configuration {
  override def toXml: Elem =
    <pluginInstance>
      <pluginName>
        {pluginName}
      </pluginName>
      <pluginAuthor>
        {pluginAuthor}
      </pluginAuthor>
      <instanceName>
        {instanceName}
      </instanceName>
    </pluginInstance>

  def this(xmlNode: Node) = this(
    (xmlNode \ "pluginName").text,
    (xmlNode \ "pluginAuthor").text,
    (xmlNode \ "instanceName").text)

}

case class ConnectorInstance(connectorType: String, sourceIdentifier: String) extends Configuration {
  override def toXml: Elem =
    <connectorInstance>
      <connectorType>
        {connectorType}
      </connectorType>
      <sourceIdentifier>
        {sourceIdentifier}
      </sourceIdentifier>
    </connectorInstance>

  def this(xmlNode: Node) = this(
    (xmlNode \ "connectorType").text,
    (xmlNode \ "sourceIdentifier").text)
}

// Insert new config options here