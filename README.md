# McMotd
[![mirai](https://img.shields.io/badge/mirai-v2.16.0-brightgreen)](https://github.com/mamoe/mirai )  
基于[mirai](https://github.com/mamoe/mirai )的Minecraft服务器信息查询插件

> 关于Linux运行环境  
> 如果你正在使用Linux而不是Windows来运行Mirai,请确保系统中安装了中文字体，否则图片可能不会被正常渲染。  

## 如何安装
1. 在[这里](https://github.com/Under-estimate/McMotd/releases/ )下载最新的插件文件。
> 使用`.mirai.jar`还是`.mirai2.jar`：  
> `mirai-console`自`2.11.0`版本起支持了[新的插件加载方式](https://github.com/mamoe/mirai/releases/tag/v2.11.0-M1)，如果您正在使用高版本`mirai-console`，则可以使用`.mirai2.jar`以避免可能的插件间依赖冲突；`.mirai.jar`为兼容插件格式，大多数版本的`mirai-console`均能使用。
2. 将插件文件放入[mirai-console](https://github.com/mamoe/mirai-console )运行生成的`plugins`文件夹中。
3. 如果您还未安装[chat-command](https://github.com/project-mirai/chat-command )插件(添加聊天环境中使用命令的功能)，你可以从下面选择一种方法安装此插件：
> 1. 如果您正在使用[Mirai Console Loader](https://github.com/iTXTech/mirai-console-loader )来启动[mirai-console](https://github.com/mamoe/mirai-console )，您可以运行以下命令来安装[chat-command](https://github.com/project-mirai/chat-command )插件：  
> `./mcl --update-package net.mamoe:chat-command --channel stable --type plugin`
> 2. 如果您没有使用[Mirai Console Loader](https://github.com/iTXTech/mirai-console-loader )，您可以在[这里](https://github.com/project-mirai/chat-command/releases )下载最新的[chat-command](https://github.com/project-mirai/chat-command )插件文件，并将其一同放入[mirai-console](https://github.com/mamoe/mirai-console )运行生成的`plugins`文件夹中。
4. 启动[mirai-console](https://github.com/mamoe/mirai-console )之后，在后台命令行输入以下命令授予相关用户使用此插件命令的权限：
> - 如果您希望所有群的群员都可以使用此插件，请输入：  
> `/perm grant m* org.zrnq.mcmotd:command.mcp` (仅可使用`mcp`指令)  
> `/perm grant m* org.zrnq.mcmotd:*` (可使用全部指令)
> - 如果您希望只授予某一个群的群员使用此插件的权限，请输入：  
> `/perm grant m<QQ群号>.* org.zrnq.mcmotd:command.mcp` (仅可使用`mcp`指令)  
> `/perm grant m<QQ群号>.* org.zrnq.mcmotd:*` (可使用全部指令)
> - 如果您希望只授予某一个群的特定群员使用此插件的权限，请输入：  
> `/perm grant m<QQ群号>.<群员QQ号> org.zrnq.mcmotd:command.mcp` (仅可使用`mcp`指令)  
> `/perm grant m<QQ群号>.<群员QQ号> org.zrnq.mcmotd:*` (可使用全部指令)
> - 如果你希望了解更多高级权限设置方法，请参阅[mirai-console的权限文档](https://github.com/mamoe/mirai-console/blob/master/docs/Permissions.md )
5. 安装完成。
## 权限列表
*有关权限部分的说明，参见[mirai-console的权限文档](https://github.com/mamoe/mirai-console/blob/master/docs/Permissions.md )*  
根权限： `org.zrnq.mcmotd:*`  
获取MC服务器信息： `org.zrnq.mcmotd:command.mcp`  
绑定服务器到群聊： `org.zrnq.mcmotd:command.mcadd`  
删除群聊绑定的服务器： `org.zrnq.mcmotd:command.mcdel`  
启动/停止服务器的在线人数记录功能： `org.zrnq.mcmotd:command.mcrec`  
获取http API访问计数： `org.zrnq.mcmotd:command.mcapi`  
重载插件配置： `org.zrnq.mcmotd:command.mcreload`
## 插件命令
> /mcp (服务器地址/服务器名称) : 查询指定地址或绑定到指定名称上的服务器信息，当本群仅绑定了一个服务器时可省略参数。  
> 
> 其中，服务器地址可以仅有域名，如`mc.example.com`，也可以带有端口号，如`mc.example.com:12345`。  
> 
> 若服务器的玩家列表信息需要通过[Query协议](https://wiki.vg/Query)获取，则需要同时指定服务器连接端口和Query端口，如`mc.example.com:12345:23456`。

> /mcadd <服务器名称> <服务器地址> : 将指定地址的服务器绑定到指定名称上。各个群聊绑定的服务器相互独立。  

> /mcdel <服务器名称> : 删除指定名称的服务器  

> /mcrec <服务器地址> (true/false) : 启动/停止对于指定服务器的在线人数记录，仅有启用了在线人数记录的服务器才会在查询结果图片中附加历史在线人数信息

> /mcapi : 获取http API访问计数信息(需要启用http API访问计数功能)  

> /mcreload : 重载插件配置
## 插件配置
插件的配置文件位于`/config/org.zrnq.mcmotd/mcmotd.yml`  

| 配置项名称                         | 配置类型                     | 说明                                                                                                                                                     |
|-------------------------------|--------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| fontName                      | 字符串(默认`Microsoft YaHei`) | 指定渲染图片时使用的字体名称                                                                                                                                         |
| showTrueAddress               | 布尔值(默认`false`)           | 设置为`true`时，服务器状态图片中显示服务器的真实地址。设置为`false`时，服务器状态图片中显示服务器的SRV地址                                                                                          |
| showServerVersion             | 布尔值(默认`false`)           | 设置为`true`时，服务器状态图片中显示服务器版本号                                                                                                                            |
| showPlayerList                | 布尔值(默认`true`)            | 设置为`true`时，服务器状态图片中显示当前在线的部分玩家(某些服务器可能不提供此信息，或提供非玩家信息的任意文本)                                                                                            |
| showPeakPlayers               | 布尔值(默认`false`)           | 设置为`true`时，服务器状态图片中将展示该服务器的历史最大在线人数（需要开启历史在线人数纪录）                                                                                                      |
| dnsServerList                 | 字符串列表                    | 指定进行SRV解析时所用的DNS服务器                                                                                                                                    |
| recordOnlinePlayer            | 字符串列表                    | 已启用历史在线人数记录的服务器                                                                                                                                        |
| recordInterval                | 整数                       | 记录在线人数的时间间隔(秒)                                                                                                                                         |
| recordLimit                   | 整数                       | 最长保留的在线人数记录时间(秒)                                                                                                                                       |
| fontPath                      | 字符串(默认为空)                | 指定渲染图片时所使用的字体文件，如果指定了字体文件并且被成功加载，则不会使用`fontName`配置项(此配置项正常情况下无需使用。如果无法使用系统字体，请使用此配置项指定字体文件([#14](https://github.com/Under-estimate/McMotd/issues/14))) |
| background                    | 字符串(默认为`#000000`)        | 指定渲染图片的背景，若以`#`开头，则指定的是RGB格式的纯色背景，否则会被解析为指向背景图片的路径                                                                                                     |
| httpServerPort                | 整数(默认为0)                 | http API的运行端口号，设置为0以禁用http API                                                                                                                         |
| httpServerMapping             | 字典(默认`{}`)               | http API中`minecraft服务器名`到`minecraft服务器地址`的对应关系                                                                                                         |
| httpServerRequestCoolDown     | 整数(默认为3000)              | http API访问冷却时间(毫秒)，设置为0以取消访问冷却                                                                                                                         |
| httpServerParallelRequest     | 整数(默认为32)                | http API最大支持的并行访问数，在没有访问冷却时间限制时，此配置项无效                                                                                                                 |
| httpServerAccessRecordRefresh | 整数(默认为0)                 | http API访问计数的统计时长(秒)，设置为0以禁用访问计数                                                                                                                       |
## HTTP API
要开启插件的HTTP API功能，需要将配置文件中的`httpServerPort`设置为非零的可用端口，并配置`httpServerMapping`。  
示例配置：
> httpServerPort: 8092  
> httpServerMapping:   
> &nbsp;&nbsp;hypixel: hypixel.net  
> &nbsp;&nbsp;earthmc: org.earthmc.net  

以上述配置启动McMotd后，访问`http://localhost:8092/info/hypixel` 将会返回与`/mcp hypixel.net`相同的图片结果；访问`http://localhost:8092/info/earthmc` 将会返回与`/mcp org.earthmc.net`相同的图片结果。访问配置文件中未定义的服务器名（如`http://localhost:8092/info/foo` ）将不会返回有效的结果
## 独立运行
McMotd支持脱离mirai-console独立运行，可以用于测试或对接其他框架。如需要独立运行McMotd，请下载[release](https://github.com/Under-estimate/McMotd/releases/)
中后缀为`.mirai.jar`的插件版本（其中包含独立运行所需的完整依赖项），并根据需求选择相应的启动命令（见下方）。
独立运行时，会在当前目录下生成配置文件`mcmotd.yml`和历史人数记录数据文件`mcmotd_data.yml`。
- 仅提供HTTP API访问功能
```bash
java -cp mcmotd-x.x.x.mirai.jar org.zrnq.mcmotd.StandaloneMainKt
```
- 仅提供图形化界面访问（用于测试，不启用历史人数记录功能）
```bash
java -cp mcmotd-x.x.x.mirai.jar org.zrnq.mcmotd.GUIMainKt
```

## FAQ
### Q: 在QQ群中发送命令没反应
A: 请检查是否安装了[chat-command](https://github.com/project-mirai/chat-command )插件，如果没有安装请看[这里](#如何安装 )

### Q: UninitializedPropertyAccessException:lateinit property FONT has not been initialized
A: 如果您正在使用Linux运行mirai，请检查是否安装了中文字体

### Q: Could not find artifact io.ktor:xxxxx:jar:2.2.2 in https://maven.aliyun.com/repository/public
A: 如果您正在使用[Mirai Console Loader](https://github.com/iTXTech/mirai-console-loader )，请在`/config/Console/PluginDependencies.yml`中添加
> &nbsp;&nbsp;\- 'https://repo.maven.apache.org/maven2/'

### Q: 访问Http API有时会返回Too Many Requests
A: 插件自`1.1.17`版本起，默认启用了Http API的访问冷却时间限制，您可以通过修改配置文件来调整冷却时间长度