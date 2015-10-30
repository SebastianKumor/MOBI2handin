char INBYTE;
int LED=13;
int var=0;

void setup() {
  // put your setup code here, to run once:
Serial.begin(9600);
pinMode(LED,OUTPUT);
Serial.println("start");
}

void loop() {
  // put your main code here, to run repeatedly:
  // Serial.println("press one to run Arduino pin 13 LED On or zero to run it OFF");
  while(!Serial.available());
  
  var = Serial.read();
  Serial.print("I received: ");
  Serial.println(var);
  //if(var == 65)analogWrite(LED, 255);
 // if(var == 175)analogWrite(LED, var);
  analogWrite(LED, var);
  delay(50);
  // Serial.println(""+INBYTE);

}
