package org.codeoverflow.chatoverflow.requirement.service.mockup

import java.util.Calendar

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.dto.chat.{Channel, ChatMessage, ChatMessageAuthor}
import org.codeoverflow.chatoverflow.connector.Connector

import scala.collection.mutable.ListBuffer
import scala.io.Source

class MockUpChatConnector(sourceIdentifier: String) extends Connector(sourceIdentifier) with WithLogger {

  private val mockUpFolder = "src/main/resources/mockup"
  private val defaultDelay = 100

  private val parser = new ChatMessageParser()
  private val messageListener = ListBuffer[ChatMessage => Unit]()
  private var messages: List[ChatMessage] = _
  private var elements: List[MockupElement] = _
  private var time: Long = 0

  def addMessageEventListener(listener: ChatMessage => Unit): Unit = {
    messageListener += listener
  }

  def addPrivateMessageEventListener(listener: ChatMessage => Unit): Unit = {
    // FIXME: Support private messages
  }

  def simulateChat(): Unit = {
    // FIXME: Should be invoked every x milliseconds using actors not using sleep
    val step = 100
    while (running) {
      val currentTime = Calendar.getInstance.getTimeInMillis

      val lastMessages = messages.filter(msg => (msg.getTimestamp > currentTime - step) && (msg.getTimestamp <= currentTime))

      lastMessages.foreach(msg => messageListener.foreach(listener => listener(msg)))

      Thread.sleep(step)
    }
  }

  def loadMockUpFile(): Boolean = {
    // TODO: Handle exceptions
    val input = Source.fromFile(s"$mockUpFolder/$sourceIdentifier.chat")
    val lines = input.getLines()


    elements = parser.parseMockUpFile(lines)
    messages = createMessageList(elements)
    input.close()
    true
    // TODO: Do not always return true
  }

  /**
    * This function is internally used to generate all chat messages form the mockup elements form the parser output.
    *
    * @param elements all mockup elements from the mockup file parser
    * @return A list of chat messages, like in a real world chat
    */
  private def createMessageList(elements: List[MockupElement]): List[ChatMessage] = {

    // FIXME: WHY (don't use a static offset you dumb brick)
    time = Calendar.getInstance().getTimeInMillis + 5000L
    logger.info(s"Started MockupChat Construction with timestamp: $time")

    logger.info(s"Started Message Creation with %d elements: %s".format(elements.size, elements.mkString(", ")))

    val messageList = ListBuffer[ChatMessage]()
    var newElements = List[ChatMessage]()
    var currentIndex = 0

    do {

      logger.info(s"New iteration from index $currentIndex.")

      val returnValue = createMessagesUntilEmptyElement(elements.toArray, currentIndex)
      newElements = returnValue._1
      currentIndex = returnValue._2
      messageList ++= newElements

      // FIXME: There might be cases where this ends up in while(true). Should be tested!
    } while (currentIndex < elements.size)

    logger.info("Finally returning %d messages.".format(messageList.size))
    messageList.toList
  }

  /**
    * This function is internally used to generate chat messages until a new line ([[EmptyElement]]) appears.
    * This function is also used to generate all messages of a loop in a recursion.
    *
    * @param allElements All elements, originally parsed from the mockup file parser
    * @param startIndex  the start index. this might be 0 for the first iteration or a higher value if there was a
    *                    new line ([[EmptyElement]] in between). Also important to re-add all messages of a loop to
    *                    the final list.
    * @return A list of chat elements and the index of the next empty element or end of array
    */
  private def createMessagesUntilEmptyElement(allElements: Array[MockupElement], startIndex: Int): (List[ChatMessage], Int) = {

    val messageList = ListBuffer[ChatMessage]()

    var isEmptyElement = false

    var index = startIndex

    // Read until the end of the parsed elements or until a empty element (empty line) appears
    while (index < allElements.length && !isEmptyElement) {

      allElements(index) match {
        case ChatElement(user, msg, isPremium) =>
          logger.info(s"Read ChatElement($user,$msg,$isPremium).")
          //FIXME isPremium is not supported by the new api implementation.
          //      Remove it or create MockupChatMessageAuthor that has field isPremium
          messageList += new ChatMessage(new ChatMessageAuthor(user), msg, time, new Channel("default"))
          time += defaultDelay
        case DelayElement(delay) =>
          logger.info(s"Read DelayElement($delay).")
          time += delay
        case RepeatElement(times) =>
          logger.info(s"Read RepeatElement($times).")
          for (i <- 0 until (times - 1)) {
            logger.info(s"Recursion $i.")
            messageList ++= createMessagesUntilEmptyElement(allElements, index + 1)._1
          }
        case EmptyElement() =>
          logger.info(s"Read EmptyElement().")
          isEmptyElement = true
      }
      index += 1
    }

    logger.info(s"Returning %d messages.".format(messageList.size))
    (messageList.toList, index)
  }

  override protected var requiredCredentialKeys: List[String] = List()

  /**
    * Starts the connector, e.g. creates a connection with its platform.
    */
  override def start(): Boolean = {
    val successful = loadMockUpFile()
    new Thread(() => simulateChat()).start()
    successful
  }

  /**
    * This stops the activity of the connector, e.g. by closing the platform connection.
    */
  override def stop(): Boolean = {
    // Nothing to do here
    true
  }

  override protected var optionalCredentialKeys: List[String] = List()
}