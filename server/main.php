<?php


/** Init **/

error_reporting(E_ALL);
define("LOG_LEVEL", 1);
require_once 'Proxy.class.php';
require_once 'MyLoger.class.php';

$myloger = new MyLoger("log.txt");
$socket = getSocket(0, 1234);
$proxys;


/** Main Loop **/

do {
    $packet = stream_socket_recvfrom($socket, 128, 0, $peer);
    if($packet == "WHOAMI") {
        stream_socket_sendto($socket, $peer, 0, $peer);
        mylog("WHO AM I : " . $peer);
    } else {
        check();
        if(!$proxys[$peer])
            $proxys[$peer] = new Proxy($socket, $peer);
        $proxys[$peer] -> receive($packet);
    }
} while (true);


/** Functions **/

function getProxy($name) {
    global $proxys;
    foreach ($proxys as $address => $proxy) {
        if($proxy -> name == $name) {
            return $proxy;
        }
    }
    return NULL;
}

function getList() {
    global $proxys;
    $list;
    foreach ($proxys as $address => $proxy) {
        if($proxy -> status >= 2) {
            $list = $list . $proxy -> name . "\n";
        }
    }
    return $list;
}

function check() {
    global $proxys;
    foreach ($proxys as $address => $proxy) {
        if(!$proxy -> check()) {
            mylog($proxys[$address] -> name . " -X- Lost !");
            unset($proxys[$address]);
        }
    }
}

function getSocket($address, $port) {
	
    $socket = stream_socket_server("udp://" . $address . ":" . $port, $errno, $errstr, STREAM_SERVER_BIND);
    
    if (!$socket) {
        die("$errstr ($errno)");
    }
    
    mylog("Server Begin...");
    
    return $socket;
}

function mylog($data) {
    global $myloger;
    $myloger -> log($data);
}
