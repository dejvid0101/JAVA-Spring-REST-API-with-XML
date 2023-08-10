var xmlrpc = require('xmlrpc');
var convert = require('xml-js');
const amqp = require('amqplib');
 
// Create RPC server, add listeners to method calls
var server = xmlrpc.createServer({ host: 'localhost', port: 9090 })
// Handle calls to nonexistent methods
server.on('NotFound', function(method, params) {
  console.log('Method ' + method + ' does not exist');
})
// Handle method calls by listening for events with the method call name
server.on('cityTemp', function (err, params, callback) {


fetch('https://vrijeme.hr/hrvatska_n.xml', {
    cache: "no-cache",
  }).then(DHMZres=>DHMZres.text())
  .then(DHMZtextres=>{
    var jsonstring=convert.xml2json(DHMZtextres, {compact: true, spaces: 0});
    var DHMZobject=JSON.parse(jsonstring);
    for(var i=0; i<DHMZobject.Hrvatska.Grad.length;i++){
        if(DHMZobject.Hrvatska.Grad[i].GradIme._text==params[0]){
             // Matching city, invoke client callback 
  callback(null, DHMZobject.Hrvatska.Grad[i].Podatci.Temp._text);
        }
    }
    
  });

 

})
console.log('XML-RPC server listening on port 9090');
sendQueueMessage();
 
// Waits briefly to give the XML-RPC server time to start up and start
// listening
setTimeout(function () {
  // Creates an XML-RPC client. Passes the host information on where to
  // make the XML-RPC calls.
  var client = xmlrpc.createClient({ host: 'localhost', port: 9090, path: '/'});

  let city='Bjelovar';
 
  // Sends a method call to the XML-RPC server
  client.methodCall('cityTemp', [city], function (error, value) {
    // Results of the method response
    console.log('Temp in ' + city +': ' + value)
  })
 
}, 1000)

// RabbitMQ client and server

async function sendQueueMessage() {
  try {
    const connection = await amqp.connect('amqp://localhost');
    const channel = await connection.createChannel();

    const queue = 'rpc_queue';
    const message = 'RPC server up and running';

    await channel.assertQueue(queue, { durable: false });
    channel.sendToQueue(queue, Buffer.from(message));

    console.log("Sent message:", message);

    setTimeout(() => {
      connection.close();
      process.exit(0);
    }, 500);
  } catch (error) {
    console.error("Error:", error);
  }
}


