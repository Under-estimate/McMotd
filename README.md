# Mirai-wiki
[![mirai](https://img.shields.io/badge/mirai-v2.10.0-brightgreen)](https://github.com/mamoe/mirai )  
基于[mirai](https://github.com/mamoe/mirai )的Minecraft服务器信息查询插件

> 关于Linux运行环境  
> 如果你正在使用Linux而不是Windows来运行Mirai,请确保Microsoft YaHei字体(msyh.ttc)已安装到你的系统中，否则汉字可能不会被正常显示。  

## 如何安装
1. 在[这里](https://github.com/Under-estimate/McMotd/releases/ )下载最新的插件文件。
2. 将插件文件放入[mirai-console](https://github.com/mamoe/mirai-console )运行生成的`plugins`文件夹中。
3. 如果您还未安装[chat-command](https://github.com/project-mirai/chat-command )插件(添加聊天环境中使用命令的功能)，你可以从下面选择一种方法安装此插件：
> 1. 如果您正在使用[Mirai Console Loader](https://github.com/iTXTech/mirai-console-loader )来启动[mirai-console](https://github.com/mamoe/mirai-console )，您可以运行以下命令来安装[chat-command](https://github.com/project-mirai/chat-command )插件：  
> `./mcl --update-package net.mamoe:chat-command --channel stable --type plugin`
> 2. 如果您没有使用[Mirai Console Loader](https://github.com/iTXTech/mirai-console-loader )，您可以在[这里](https://github.com/project-mirai/chat-command/releases )下载最新的[chat-command](https://github.com/project-mirai/chat-command )插件文件，并将其一同放入[mirai-console](https://github.com/mamoe/mirai-console )运行生成的`plugins`文件夹中。
4. 启动[mirai-console](https://github.com/mamoe/mirai-console )之后，在后台命令行输入以下命令授予相关用户使用此插件命令的权限：
> - 如果您希望所有群的群员都可以使用此插件，请输入：  
> `/perm grant m* org.zrnq.mcmotd:*`  
> - 如果您希望只授予某一个群的群员使用此插件的权限，请输入：  
> `/perm grant m<QQ群号>.* org.zrnq.mcmotd:*`
> - 如果您希望只授予某一个群的特定群员使用此插件的权限，请输入：  
> `/perm grant m<QQ群号>.<群员QQ号> org.zrnq.mcmotd:*`
> - 如果你希望了解更多高级权限设置方法，请参阅[mirai-console的权限文档](https://github.com/mamoe/mirai-console/blob/master/docs/Permissions.md )
5. 安装完成。
## 权限列表
*有关权限部分的说明，参见[mirai-console的权限文档](https://github.com/mamoe/mirai-console/blob/master/docs/Permissions.md )*  
根权限： `org.zrnq.wiki:*`  
基本操作权限： `org.zrnq.wiki:command.wiki`
- 包含所有命令执行的权限。
## 插件命令
> mcp <服务器地址> : 查询指定地址上的服务器信息

其中，服务器地址可以仅有域名，如`mc.example.com`，也可以带有端口号，如`mc.example.com:12345`

## FAQ
### Q: 在QQ群中发送命令没反应
A: 请检查是否安装了[chat-command](https://github.com/project-mirai/chat-command )插件，如果没有安装请看[这里](#如何安装 )