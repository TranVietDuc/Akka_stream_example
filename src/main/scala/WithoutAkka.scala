object SampleData {
  case class Account(id: Int, name: String)
  case class Campaign(id: Int, accountId: Int, name:String)
  case class Keyword(id: Int, campaignId: Int, name: String)

  val sampleAccounts:List[Account] = List(
    Account(1, "JungKook"),
    Account(2, "Sugar"),
    Account(3, "Jimin")
  )

  val sampleCampaigns: List[Campaign] = List(
    Campaign(1, 1 , "JungKook-campaign-1"),
    Campaign(2, 1 , "JungKook-campaign-2"),
    Campaign(3, 2 , "Sugar-campaign-1"),
    Campaign(4, 3 , "V-campaign-1"),
  )

  val sampleKeywords: List[Keyword] = List(
    Keyword(1, 1, "A"),
    Keyword(2, 1, "B"),
    Keyword(3, 1, "C"),
    Keyword(4, 1, "D"),
    Keyword(5, 1, "E"),
    Keyword(6, 2, "F"),
    Keyword(7, 2, "G"),
    Keyword(8, 2, "H"),
    Keyword(9, 3, "I"),
    Keyword(10, 4, "K"),
  )
  def fakeInsert(insertState: String): Unit = {
    println(insertState)
    val start = 500
    val end   = 1500
    val rnd = new scala.util.Random
    val randomTime = start + rnd.nextInt( (end - start) + 1 )
    Thread.sleep(randomTime)
  }
}

import SampleData._

import scala.language.postfixOps

object WithoutAkka extends App {
  sampleAccounts foreach(account => {
    fakeInsert(s"Insert account ${account.name}")
    sampleCampaigns.filter(campaign => campaign.accountId == account.id) foreach (cpg => {
      fakeInsert(s"Insert campaign ${cpg.name}")
      sampleKeywords.filter(keyWord => keyWord.campaignId == cpg.id) foreach( kw => {
        fakeInsert(s"Insert keyWords ${kw.name}")
      })
    })
  })
}



