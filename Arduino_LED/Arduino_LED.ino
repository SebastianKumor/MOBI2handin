int LED=13;
int var=0;

void setup() {
  Serial.begin(9600);
  pinMode(LED,OUTPUT);
  Serial.println("start");
}

void loop() {
  while(!Serial.available());
  
  var = Serial.read() - '0';
  analogWrite(LED, var);
  delay(50);
}
