object ScalaTest extends App {
  sealed trait Keyboard
  case class Stock(vendor: String, price: Double, color: String) extends Keyboard
  case class Custom(maker: String, price: Double, color: String) extends Keyboard

  def showOff(keyboard: Keyboard): String = {
    keyboard match {
      case Stock(vendor, _ , _ ) => s"Stock vendor: ${vendor}"
      case Custom(maker, price, color) => s"Custom: maker: $maker price: $price color: $color"
    }
  }
  val newStock = Stock("CJ", 2000, "black")
  val newCustom = Custom("SM", 2300, "red")
  println(showOff(newCustom))
  println(showOff(newStock))
  val Stock(vendorA, _, _ ) = newStock
  println(vendorA)

  def toYesOrNo (choise: Int): String = choise match {
    case 0 => "no"
    case 1 | 2 | 3 => "yes"
    case _ =>  "error"
  }

  println(toYesOrNo(1))

  def f(x: Any): String = x match {
    case i: Int => "integer: " + i
    case d: Double => "Double: " + d
    case s: String => "String: " + s
    case _ => "Incorrect type"
  }

  def fizzBuzz(a: Int): Unit = {
    (1 to a).map(i => (i%3, i%5) match {
      case (0, 0) => "FizzBuzz"
      case (0, _) => "Fizz"
      case (_, 0) => "Buzz"
      case _ => i
    }).foreach(a => print(a + " "))
  }

  fizzBuzz(20)




}
