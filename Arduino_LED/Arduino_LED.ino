int LED=13;
int var=0;

void setup() {
  Serial.begin(9600);
  pinMode(LED,OUTPUT);
  Serial.println("start");
}

void loop() {
  if(Serial.available()){
    var = Serial.read() - '0';
  }
  digitalWrite(LED, HIGH);
  delay(var*100);
  digitalWrite(LED, LOW);
  delay(var*100);
}
