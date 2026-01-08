package cn.pylin.xycjd;

interface IUserShellService {
    void destroy() = 16777114;  // Shizuku 内置销毁方法
    String execLine(String command) = 1; // 执行单行命令
}