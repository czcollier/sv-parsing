package com.skyhookwireless.parsing

import org.joda.time.DateTime

case class LocationSample(
  partnerID: String,
  locationTS: DateTime,
  processDate: DateTime,
  userID: String,
  observationTS: Option[DateTime] = None,
  ipAddress: Option[Long] = None,
  lat: Option[Double] = None,
  lon: Option[Double] = None,
  campaignID: Option[String] = None,
  publisherID: Option[String] = None,
  secondaryID: Option[String] = None,
  locationSource: Option[String] = None,
  wifiMac: Option[String] = None,
  wifiSsid: Option[String] = None,
  debugMode: Option[String] = None,
  otherAttributes: Option[String] = None)
