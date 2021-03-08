#include <LiquidCrystal_I2C.h> //LiquidCrystal_I2C의 라이브러리를 불러옵니다.(I2C LCD)
#include <Wire.h> //DHT의 라이브러리
#include <DHT.h> //DHT의 라이브러리
#include <SoftwareSerial.h> //hc06의 라이브러리
#include <Timer.h> //타이머 라이브러리

#define DHTPIN 2
#define DHTTYPE DHT22
DHT dht(DHTPIN, DHTTYPE); //dht 설정

LiquidCrystal_I2C lcd(0x27, 16, 2);   //lcd(LCD의 I2C 슬레이브 주소, lcd 1줄당 출력할 글자수, lcd 줄의 수)
SoftwareSerial hc06(11, 12); //hc06(TXD. RXD)
Timer Ts; //타이머 변수

int pin = 8; //미세먼지 핀번호
int FAN = A1; //Fan 핀번호

unsigned long duration; //지속시간
unsigned long starttime; //시작시간
unsigned long sampleDusttime_ms = 10000; //먼지 샘플시간 10초마다 업데이트
unsigned long sampleTemtime_ms = 10000;  //온도 샘플시간 20초마다 업데이트
unsigned long lowpulseoccupancy = 0;   //Low 신호가 지속된 시간을 초기화
float ratio = 0; //비율 0으로 초기화
float concentration = 0;  //입자 농도 0으로 초기화
float ugm3 = 0;   //최종 값으로 세제곱미터 당 마이크로 그램(㎍/㎥) 초기화

void setup() {

  Serial.begin(9600); //시리얼 통신 시작
  hc06.begin(9600); //블루투스 통신 시작
  dht.begin();  //온습도 통신 시작

  lcd.init();  //LCD_I2C 통신 시작
  lcd.backlight();   //LCD backlight를 ON

  Serial.println("Start"); //시리얼 모니터에 Start 출력

  pinMode(pin, INPUT); 
  starttime = millis();
  
  pinMode(FAN, OUTPUT);
  digitalWrite(FAN, LOW); //핀모드 설정

  /*
    every 함수를 통해 몇초동안 어떤 함수를 실행시킬 것인지 설정
    첫번째 매개변수는 업데이트 할 시간 두번째 매개변수는 실행시킬 함수명
  */

  Ts.every(sampleDusttime_ms, doDust);
  Ts.every(sampleTemtime_ms, doTemperatur);
}

void doDust() {
  
  String dust = String(ugm3);
  dust.concat("#");
  hc06.println(dust);  //블루투스에 먼지농도값 전달

  lcd.clear();        //lcd 화면을 지움
  lcd.home();        //lcd 커서 위치를 0,1로 위치
  if ( ugm3 > 150 ) {  //매우 나쁨
    lcd.print("AIR : VERY BAD!!");
  } else if ( ugm3 > 80) { //나쁨
    lcd.print("AIR : BAD!      ");
  } else if ( ugm3 > 30) { //보통
    lcd.print("AIR : NORMAL     ");
  } else {                 //좋음
    lcd.print("AIR : GOOD      ");
  }
  lcd.setCursor(0, 1);  //lcd 커서의 위치를 4,0으로 설정합니다.
  lcd.print("ug/m3:");  //현재 lcd 커서 위치로부터 내용 출력
  lcd.print(ugm3);  //lcd에 먼지농도 출력
}

void doTemperatur() {

  float t = dht.readTemperature();  //온도값을 읽어옴
  float h = dht.readHumidity(); //습도값을 읽어옴.

  String tempstr = String(t);
  tempstr.concat("!");
  hc06.println(tempstr);  //블루투스에 온도값 전달

  delay(1000);
  
  String humistr = String(h);
  humistr.concat("%");
  hc06.println(humistr); //블루투스에 습도값 전달

  Serial.print("Humidity: ");
  Serial.print(h);
  Serial.print(" %\t");
  Serial.print("Temperature: ");
  Serial.print(t);
  Serial.print(" *C "); //시리얼 모니터에 온습도 출력
}


void loop() {

  duration = pulseIn(pin, LOW);
  lowpulseoccupancy = lowpulseoccupancy + duration;
  if ((millis() - starttime) > sampleDusttime_ms)
  {
    ratio = lowpulseoccupancy / (sampleDusttime_ms * 10.0);
    concentration = 1.1 * pow(ratio, 3) - 3.8 * pow(ratio, 2) + 520 * ratio + 0.62;
    ugm3 = concentration * 100 / 13000;
    Serial.print(ugm3);
    Serial.println("ug/m3");
    lowpulseoccupancy = 0;
    starttime = millis();
  } //먼지 농도 계산
  
  Ts.update(); // dustTs변수에 지정했던 every함수에서 설정했던 값을 실행

  char cmd = (char)hc06.read();
  if(cmd == '1'){ digitalWrite(FAN, LOW);}
  if(cmd == '2'){ digitalWrite(FAN, HIGH);} //어플의 버튼 on/off에 따라 환풍팬 실행

}
