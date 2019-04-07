package anewlife.adolf

import java.io.{ByteArrayOutputStream, File, PrintStream}

import org.javacord.api.entity.channel.TextChannel
import org.javacord.api.entity.message.embed.EmbedBuilder
import org.javacord.api.entity.message.{Message, MessageBuilder, MessageDecoration}
import org.javacord.api.event.message.MessageCreateEvent

import scala.collection.mutable
import scala.reflect.internal.util.{AbstractFileClassLoader, BatchSourceFile}
import scala.reflect.io.AbstractFile
import scala.tools.nsc.{Global, Settings}
import scala.tools.nsc.io.VirtualDirectory
import collection.JavaConverters._
import scala.language.implicitConversions

object ExecInContext {


  val deferr = System.err

  def exec(evt: MessageCreateEvent): Unit = {
    val content = evt.getMessageContent
    val quote= "(?u),,\\s*(.*?),,(.*)".r
    val rawcode = "(?u)st (.*)".r
    content match {
      case quote(last, rep) => replyQuote(last, "", rep, evt)
      case rawcode(raw) => execRaw(evt, raw)
      case _ =>
    }
  }


  def replyQuote(last: String, start: String, rep: String, evt: MessageCreateEvent): Unit = {
    findMessage(last, start, rep, evt)
      .ifPresent(replyToMessage(_, rep, evt))
  }

  def findMessage(last: String, start: String, rep: String, evt: MessageCreateEvent) = {
    val matchCase = s".*?($start.*$last).{0,7}"
    evt.getChannel
      .getMessagesBeforeAsStream(evt.getMessageId)
      .filter(m => m.getContent.matches(matchCase))
      .findFirst()
  }

  def replyToMessage(m:Message, rep:String, evt: MessageCreateEvent): Unit = {
    new MessageBuilder()
      .append(">>")
      .append(m.getAuthor)
      .append(">>")
      .appendNewLine()
      .append("```" + m.getContent + "```")
      .appendNewLine()
      .append(rep)
      .send(evt.getChannel)
  }

  private def execRaw(evt: MessageCreateEvent, code: String) = {
    val berr = new ByteArrayOutputStream()
    System.setErr(new PrintStream(berr))
    val msg = new MessageBuilder
    try {
      val compiler = new Compiler(None)
      val script = compiler.compile(code).getConstructor(classOf[MessageCreateEvent])
        .newInstance(evt).asInstanceOf[ScalaScriptContainer]
      val returnValue = script.apply()
      msg.append("debug: ")
      msg.append(returnValue)
    } catch {
      case e: Exception => e.printStackTrace(); msg.append(berr.toString("utf-8"), MessageDecoration.ITALICS)
    } finally {
      msg.send(evt.getChannel)
      System.setErr(deferr)
    }
  }

  abstract class ScalaScriptContainer(evt: MessageCreateEvent) extends (() => Any) {
    implicit val e: MessageCreateEvent = evt
    def cit(id: Long): String =
      "```" + e.getChannel.getMessageById(id).get().getContent + "```"
  }

  class Compiler(targetDir: Option[File]) {
    var classc = 0


    val target = targetDir match {
      case Some(dir) => AbstractFile.getDirectory(dir)
      case None => new VirtualDirectory("(memory)", None)
    }

    val classCache = mutable.Map[String, Class[_]]()

    private val settings = new Settings()
    settings.deprecation.value = true // enable detailed deprecation warnings
    settings.unchecked.value = true // enable detailed unchecked warnings
    settings.outputDirs.setSingleOutput(target)
    settings.usejavacp.value = true
    //  settings.target.value = "jvm-1.8"


    private val global = new Global(settings)
    private lazy val run = new global.Run

    val classLoader = new AbstractFileClassLoader(target, this.getClass.getClassLoader)

    /** Compiles the code as a class into the class loader of this compiler.
      *
      * @param code
      * @return
      */
    def compile(code: String) = {
      val className = classNameForCode(code)

      findClass(className).getOrElse {

        val sourceFiles = List(new BatchSourceFile("(inline)", wrapCodeInClass(className, code)))
        run.compileSources(sourceFiles)
        findClass(className).get
      }
    }

    /** Compiles the source string into the class loader and
      * evaluates it.
      *
      * @param code
      * @tparam T
      * @return
      */
    def eval[T](code: String): T = {
      val cls = compile(code)
      cls.getConstructor().newInstance().asInstanceOf[() => Any].apply().asInstanceOf[T]
    }

    def findClass(className: String): Option[Class[_]] = {
      synchronized {
        classCache.get(className).orElse {
          try {
            val cls = classLoader.loadClass(className)
            classCache(className) = cls
            Some(cls)
          } catch {
            case e: ClassNotFoundException => None
          }
        }
      }
    }

    protected def classNameForCode(code: String): String = {
      classc = classc + 1
      "S͟c͟a͟l͟a͟_S͟c͟r͟i͟p͟t͟_ೠ" + classc + "ဪ"
    }

    /*
    * Wrap source code in a new class with an apply method.
    */
    private def wrapCodeInClass(className: String, code: String) = {
        "import org.javacord.api.event.message.MessageCreateEvent\n" +
        "import rx.lang.scala.JavaConversions._\n    " +
        "import collection.JavaConverters._\n    " +
        "import scala.collection.mutable\n    " +
        "import java.util\n    " +
        "import java.util.concurrent.TimeUnit\n" +
        "import org.javacord.api.entity.message._ \n" +
      "import org.javacord.api.entity.message.embed._\n" +
        " class " + className + "(evt: MessageCreateEvent) extends anewlife.adolf.ExecInContext.ScalaScriptContainer(evt) {\n" +
        "  def apply() = {\n" +
        code + "\n" +
        "  }\n" +
        "}\n"
    }
  }

}
