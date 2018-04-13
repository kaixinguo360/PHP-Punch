<?php
class MyLoger {
    
    /* 成员变量 */
    var $fh;
    
    /* 构造函数 */
    function __construct($logFile) {
        global $fh;
        $fh = fopen($logFile, 'w');
    }
    
    /* 析构函数 */
    function __destruct(){
        global $fh;
        fwrite ($fh, "\nEnd at: ".time());
        fclose ($fh);
    }
    
    /* 成员函数 */
    function log($data) {
        global $fh;
        $data = date('Y-m-d H:i:s') . "/" . $data;
        echo $data . "<br>";
        fwrite($fh, $data . "\n");
    }
}
