package test_data_generator

import java.time.{LocalDate, LocalDateTime, ZoneOffset}
import java.time.temporal.ChronoUnit.{DAYS, SECONDS}

import scala.util.Random


object SampleGenerator {
  private val random = new Random(System.nanoTime)

  def between(from: Int, to: Int): Int = {
    from + Random.nextInt(to - from + 1)
  }

  def int_generator(count: Int, lower: Int, upper: Int, start: Int, repeat: Int = 1, step: Int = 1): List[Int] = {
    if (start != 0) {
      List.range(start, start + (((count / repeat) + 1) * step), step).flatMap(x => List.fill(repeat)(x)).take(count)
    }
    else List.fill(count)(between(lower, upper))
  }

  def between(from: LocalDate, to: LocalDate): LocalDate = {
    from.plusDays(random.nextInt(DAYS.between(from, to).toInt))
  }

  def between(from: LocalDateTime, to: LocalDateTime): Long = {
    from.plusSeconds(random.nextInt(SECONDS.between(from, to).toInt)).atZone(ZoneOffset.UTC).toInstant().toEpochMilli() / 1000
  }

  def datetime(from: LocalDateTime, step_by_minute: Int, count: Int): List[Long] = {
    List.range(0, count).map(x => from.plusMinutes(x * step_by_minute).atZone(ZoneOffset.UTC).toInstant().toEpochMilli() / 1000)
  }

  def random(count: Int, lenght: Int = 4): List[String] = List.fill(count)(Random.alphanumeric.filter(_.isLetter).take(lenght).mkString)

  def random[A](count: Int, values: List[A]): List[A] = List.fill(count)(Random.shuffle(values).head)

  def random_bool(count: Int): List[Boolean] = List.fill(count)(Random.nextBoolean)

}
