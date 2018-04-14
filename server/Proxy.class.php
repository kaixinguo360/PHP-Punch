<?php
class Proxy {
    
    /* 成员变量 */
    var $address, $socket, $myloger, $status, $name;
    
    /* 构造函数 */
    function __construct($socket1, $address1, $myloger1) {
        global $address, $socket, $status;
        $address = $address1;
        $socket = $socket1;
        $myloger = $myloger1;
        $status = 0;
        mylog("User " . $address . " Login");
    }
    
    /* 成员函数 */
    function receive($packet){
        global $address;
        mylog($address . " --> " . $packet);
        $this -> check($packet);
    }
    
    function check($data){
        global $name;
        if($data == "SYN") {
            if($this -> status == 0) {
                $this -> status = 1;
                $this -> send("ACK");
            } else {
                $this -> status = 0;
            }
        }
        if($this -> status == 1) {
            $str = substr($data,0, 3);
            $name1 = substr($data,3);
            if($str == "ACK") {
                $this -> name = $name1;
                $this -> status = 2;
                $this -> send("ACCEPT");
            }
        }
        if($this -> status == 2 && $data == "OK") {
            $this -> status = 3;
            $this -> send("OK");
        }
    }
    
    function send($packet){
        global $address, $socket;
        mylog($address . " <-- " . $packet);
        stream_socket_sendto($socket, $packet, 0, $address);
    }
}
