import Dependencies._
import com.typesafe.config.Config
import com.typesafe.sbt.packager.docker.Cmd
import sbt._

import scala.util.Try

object DockerProperties extends BuildConf {

  import scala.collection.JavaConverters._

  val fileName = ".docker.build.conf"

  private val defaultCommands: Seq[Cmd] = Seq(
    Cmd("USER", "root"),
    Cmd("RUN", "echo \"deb http://repos.mesosphere.io/debian jessie main\" | tee /etc/apt/sources.list.d/mesosphere.list"),
    Cmd("RUN", "apt-key adv --keyserver keyserver.ubuntu.com --recv E56151BF"),
    Cmd("RUN", s"apt-get update --fix-missing && apt-get install -y --no-install-recommends  mesos=$mesosVersion-0.2.62.debian81"),
    Cmd("ENV", s"MESOS_JAVA_NATIVE_LIBRARY /usr/local/lib/libmesos-$mesosVersion.so"),
    Cmd("ENV", s"MESOS_LOG_DIR /var/log/mesos")
  )
  private val defaultVolumes: Seq[String] = Seq("/opt/docker", "/opt/docker/notebooks", "/opt/docker/logs")

  private def asCmdSeq( configs: Seq[Config] ): Seq[Cmd] = {
    configs.flatMap { possibleCmd =>
      val cmd = Try { Some(possibleCmd.getString("cmd")) }.getOrElse(None)
      val arg = Try { Some(possibleCmd.getString("arg")) }.getOrElse(None)
      ( cmd, arg ) match {
        case (Some(c), Some(a)) => Some( Cmd(c,a) )
        case _ => None
      }
    }
  }

  val maintainer   = getString("docker.maintainer", "Andy Petrella")

  val baseImage    = getString("docker.baseImage", "java:8-jdk")

  val commands     = Try { asCmdSeq(cfg.getConfigList("docker.commands").asScala.toSeq) }.getOrElse( defaultCommands )
  val volumes      = Try { cfg.getStringList("docker.volumes").asScala.toSeq }.getOrElse( defaultVolumes )
  val registry     = Some(getString("docker.registry", "andypetrella"))
  val ports        = Try { cfg.getIntList("docker.ports").asScala.toSeq.map { e => e.intValue() } }.getOrElse( Seq(9000, 9443) )
}
