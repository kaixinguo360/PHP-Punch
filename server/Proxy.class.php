<?php
class Proxy {
    
    /* 成员变量 */
    var $address, $socket, $myloger;
    
    /* 构造函数 */
    function __construct($socket1, $address1, $myloger1) {
        global $address, $socket;
        $address = $address1;
        $socket = $socket1;
        $myloger = $myloger1;
        mylog("User " . $address . " Login");
    }
    
    /* 成员函数 */
    function receive($packet){
        global $address, $socket;
        mylog("<-- " . $address . ":" . $packet);
        $this -> send($whoami . '/' . date('Y-m-d H:i:s') . '/' . $address);
    }
    
    function send($packet){
        global $address, $socket;
        mylog("--> " . $address . ":" . $packet);
        stream_socket_sendto($socket, $packet, 0, $address);
    }
}
