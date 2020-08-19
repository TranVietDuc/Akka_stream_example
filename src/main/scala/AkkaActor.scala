import AkkaActor.AccountActor
import AkkaActor.AccountActor.ProcessAccount
import AkkaActor.CampaignActor.ProcessCampaign
import AkkaActor.KeywordActor.ProcessKeyword
import SampleData._
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.routing.RoundRobinPool

object AkkaActor {

  object KeywordActor {
    def props: Props = Props[KeywordActor]
    case class ProcessKeyword(keyword: Keyword)
  }

  class KeywordActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case ProcessKeyword(keyword) =>
        log.info(s"on keyword ${keyword.name}")
        fakeInsert(s"Insert keyWords ${keyword.name}")
    }
  }

  object CampaignActor {
    def props: Props = Props[CampaignActor]
    case class ProcessCampaign(campaign: Campaign)
  }
  class CampaignActor extends Actor with ActorLogging {

    override def preRestart(reason: Throwable, message: Option[Any]): Unit = super.preRestart(reason, message)

    val keywordActor1: ActorRef = context.actorOf(RoundRobinPool(1).props(KeywordActor.props), "keyword")

    override def receive: Receive = {
      case ProcessCampaign(campaign) =>
        log.info(s"on campaign ${campaign.name}")
        fakeInsert(s"Insert campaign ${campaign.name}")
        sampleKeywords.filter(kw => kw.campaignId == campaign.id) foreach (keyWord => {
          keywordActor1 ! ProcessKeyword(keyWord)
        })
    }
  }

  object AccountActor {
    def props: Props = Props[AccountActor]
    case class ProcessAccount(account: Account)
  }

  class AccountActor extends Actor with ActorLogging{

    val campaignActor: ActorRef = context.actorOf(RoundRobinPool(1).props(CampaignActor.props), "adGroup")

    override def receive: Receive = {
      case ProcessAccount(account) =>
        log.info(s"on account ${account.name}")
        fakeInsert(s"Insert account ${account.name}")
        sampleCampaigns.filter(cpg => cpg.accountId == account.id) foreach (campaign => {
          campaignActor ! ProcessCampaign(campaign)
        })}
  }
}

object AkkaActorRun extends App {
  val system: ActorSystem = ActorSystem("BatchAkka")
  val accountActor = system.actorOf(AccountActor.props, "account")

  sampleAccounts.foreach(accountActor ! ProcessAccount(_))

}
