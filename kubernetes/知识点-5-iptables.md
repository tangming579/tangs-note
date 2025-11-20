# 概念

`iptables` 是 Linux 系统中自带的开源包过滤工具，它允许用户设置、维护和检查网络流量的过滤规则。

现在 redhat 系统默认使用`firewalld`，`ubuntu`默认使用`ufw`防火墙，但在容器环境，Docker 默认直接管理 iptables 规则来实现网络隔离和端口映射。

ipatbles中的基本概念

1. **表（Tables）**：`iptables` 中的规则被分为不同的表，每个表有不同的处理目标。常见的表包括：
   - **filter**：默认表，用于处理网络流量的过滤。
   - **nat**：用于网络地址转换，处理源地址和目标地址的转换。
   - **mangle**：用于修改数据包的某些属性，如 TOS（服务类型）字段。
   - **raw**：用于处理数据包的原始数据，通常用于调试目的。
2. **链（Chains）**：每个表中包含多个链，每条链包含一组规则。常见的链包括：
   - **INPUT**：处理进入本地系统的数据包。
   - **FORWARD**：处理经过本地系统转发的数据包。
   - **OUTPUT**：处理从本地系统发出的数据包。
   - **PREROUTING**：在路由决策之前处理数据包，用于 NAT 和 Mangle 表。
   - **POSTROUTING**：在路由决策之后处理数据包，用于 NAT 和 Mangle 表。
3. **规则（Rules）**：每条规则定义了如何处理匹配特定条件的数据包。规则包括匹配条件和相应的动作（如 ACCEPT、DROP、REJECT 等）。

## 常见应用场景

| 应用类型      | 使用的表 | 使用的链                |
| :------------ | :------- | :---------------------- |
| 防火墙过滤    | filter   | INPUT, FORWARD, OUTPUT  |
| 地址转换(NAT) | nat      | PREROUTING, POSTROUTING |
| 数据包修改    | mangle   | 全部五链                |
| 关闭连接跟踪  | raw      | PREROUTING, OUTPUT      |