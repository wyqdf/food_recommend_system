开发者提供对象存储 OSS 的 API 概览，帮助理解其核心概念与资源模型，并快速查找所需接口。如果要进行快速二次开发，建议在生产环境中使用SDK开发包，其封装了复杂的签名、重试和并发逻辑，本文可作为 SDK 底层实现原理的参考，适用于需要深度定制或理解通信机制的场景。

开始使用前，请注意：

API 的使用须遵循OSS使用限制。

建议在正式接入前了解OSS的计费说明，定价详情见OSS产品定价。

快速上手：调用第一个API
以创建存储空间（PutBucket）为例，介绍一次完整的API调用流程。

1. 准备工作
开始前，需获取以下信息：

AccessKey：为保证访问安全，除匿名访问外的所有 OSS API 请求都必须经过签名认证。OSS 使用基于AccessKey（AccessKey ID 和 AccessKey Secret）的签名机制验证请求。签名信息需通过 HTTP 请求头中的 Authorization 字段传递，计算方法见签名版本4（推荐）。

Endpoint：API 请求必须发送到目标存储空间所在地域的 Endpoint，Endpoint是访问OSS服务的入口地址，各地域对应的 Endpoint 详情见地域和Endpoint。

2. 构造并发送请求
放大查看复制代码
PUT / HTTP/1.1
Host: oss-example.oss-cn-hangzhou.aliyuncs.com
Date: Thu, 17 Apr 2025 03:15:40 GMT
x-oss-acl: private
Authorization: OSS4-HMAC-SHA256 Credential=LTAI********************/20250417/cn-hangzhou/oss/aliyun_v4_request,Signature=a7c3554c729d71929e0b84489addee6b2e8d5cb48595adfc51868c299c0c218e
<?xml version="1.0" encoding="UTF-8"?>
<CreateBucketConfiguration>
    <StorageClass>Standard</StorageClass>
    <DataRedundancyType>LRS</DataRedundancyType>    
</CreateBucketConfiguration>
3. 理解响应结果
成功响应：请求成功后，服务器会返回 2xx 状态码。对于有返回内容的操作，响应体为 XML 格式。

放大查看复制代码
HTTP/1.1 200 OK
x-oss-request-id: 534B371674E88A4D8906****
Date: Fri, 24 Feb 2017 03:15:40 GMT
Content-Length: 0
Connection: keep-alive
Server: AliyunOSS
Location: /oss-example
错误处理：请求失败时，服务器会返回 4xx 或 5xx 状态码。响应体同样为 XML 格式，其中包含具体的错误码（Code）和错误信息（Message），可参考OSS错误码定位问题

API 列表
关于Service操作
放大查看
API

描述

ListBuckets（GetService）

返回请求者拥有的所有存储空间（Bucket）。

ListUserDataRedundancyTransition

列举请求者所有的存储冗余转换任务。

关于Region操作
放大查看
API

描述

DescribeRegions

查询所有支持地域或者指定地域对应的Endpoint信息。

关于Bucket操作
放大查看
分类

API

描述

基础操作

PutBucket

创建Bucket。

DeleteBucket

删除Bucket。

ListObjects（GetBucket）

列出Bucket中所有文件（Object）的信息。

ListObjectsV2（GetBucketV2）

GetBucketInfo

获取Bucket信息。

GetBucketLocation

获取Bucket所属的位置信息。

GetBucketStat

获取Bucket的存储容量以及Object数量。

合规保留策略（WORM）

InitiateBucketWorm

新建合规保留策略。

AbortBucketWorm

删除未锁定的合规保留策略。

CompleteBucketWorm

锁定合规保留策略。

ExtendBucketWorm

延长已锁定的合规保留策略对应Bucket中Object的保留天数。

GetBucketWorm

获取Bucket的合规保留策略信息。

权限控制（ACL）

PutBucketAcl

设置Bucket访问权限。

GetBucketAcl

获取Bucket访问权限。

生命周期（Lifecycle）

PutBucketLifecycle

设置Bucket中Object的生命周期规则。

GetBucketLifecycle

获取Bucket中Object的生命周期规则。

DeleteBucketLifecycle

删除Bucket中Object的生命周期规则。

传输加速（TransferAcceleration）

PutBucketTransferAcceleration

为存储空间（Bucket）配置传输加速。

GetBucketTransferAcceleration

获取目标存储空间（Bucket）的传输加速配置。

版本控制（Versioning）

PutBucketVersioning

设置Bucket的版本控制状态。

GetBucketVersioning

获取Bucket的版本控制状态。

ListObjectVersions（GetBucketVersions）

列举Bucket中所有Object的版本信息。

数据复制（Replication）

PutBucketReplication

设置Bucket的数据复制规则。

PutBucketRTC

为已有的跨区域复制规则开启或关闭数据复制时间控制（RTC）功能。

GetBucketReplication

查看Bucket已设置的数据复制规则。

GetBucketReplicationLocation

查看可复制到的目标Bucket所在的地域。

GetBucketReplicationProgress

查看Bucket的数据复制进度。

DeleteBucketReplication

停止Bucket的数据复制任务并删除Bucket的复制配置。

授权策略（Policy）

PutBucketPolicy

设置Bucket Policy。

GetBucketPolicy

获取Bucket Policy。

GetBucketPolicyStatus

查看当前Bucket Policy是否允许公共访问。

DeleteBucketPolicy

删除Bucket Policy。

清单（Inventory）

PutBucketInventory

设置Bucket清单规则。

GetBucketInventory

查看Bucket中指定的清单任务。

ListBucketInventory

查看Bucket中所有的清单任务。

DeleteBucketInventory

删除Bucket中指定的清单任务。

日志管理（Logging）

PutBucketLogging

开启Bucket访问日志记录功能。

GetBucketLogging

查看Bucket的访问日志配置情况。

DeleteBucketLogging

关闭Bucket访问日志记录功能。

PutUserDefinedLogFieldsConfig

为Bucket实时日志中的user_defined_log_fields字段进行个性化配置。

GetUserDefinedLogFieldsConfig

获取Bucket实时日志中的user_defined_log_fields字段进行个性化配置。

DeleteUserDefinedLogFieldsConfig

删除Bucket实时日志中的user_defined_log_fields字段进行个性化配置。

静态网站（Website）

PutBucketWebsite

设置Bucket为静态网站托管模式。

GetBucketWebsite

查看Bucket的静态网站托管状态。

DeleteBucketWebsite

关闭Bucket的静态网站托管模式。

防盗链（Referer）

PutBucketReferer

设置Bucket的防盗链规则。

GetBucketReferer

查看Bucket的防盗链规则。

标签（Tags）

PutBucketTags

添加或修改Bucket标签。

GetBucketTags

查看Bucket标签信息。

DeleteBucketTags

删除Bucket标签。

加密（Encryption）

PutBucketEncryption

配置Bucket的加密规则。

GetBucketEncryption

获取Bucket的加密规则。

DeleteBucketEncryption

删除Bucket的加密规则。

请求者付费（RequestPayment）

PutBucketRequestPayment

设置Bucket为请求者付费模式。

GetBucketRequestPayment

查看Bucket请求者付费模式配置信息。

跨域资源共享（CORS）

PutBucketCors

为指定的存储空间（Bucket）设置跨域资源共享CORS（Cross-Origin Resource Sharing）规则。

GetBucketCors

获取指定存储空间（Bucket）当前的跨域资源共享CORS规则。

DeleteBucketCors

关闭指定存储空间（Bucket）对应的跨域资源共享CORS功能并清空所有规则。

Options

浏览器在发送跨域请求之前会发送一个preflight请求（Options）给OSS，并带上特定的来源域、HTTP方法和header等信息，以决定是否发送真正的请求。

访问跟踪（AccessMonitor）

PutBucketAccessMonitor

配置Bucket的访问跟踪状态。

GetBucketAccessMonitor

获取Bucket的访问跟踪状态。

数据索引（Data Indexing）

OpenMetaQuery

为Bucket开启元数据管理功能。

GetMetaQueryStatus

获取指定Bucket的元数据索引库信息。

DoMetaQuery

查询满足指定条件的Object，并按照指定字段和排序方式列出Object信息。

CloseMetaQuery

关闭Bucket的元数据管理功能。

高防（DDoS Protection）

InitUserAntiDDosInfo

创建高防OSS实例。

UpdateUserAntiDDosInfo

更改高防OSS实例状态。

GetUserAntiDDosInfo

查询指定账号下的高防OSS实例信息。

InitBucketAntiDDosInfo

初始化Bucket防护。

UpdateBucketAntiDDosInfo

更新Bucket防护状态。

ListBucketAntiDDosInfo

获取Bucket防护信息列表。

资源组

PutBucketResourceGroup

为Bucket配置所属资源组。

GetBucketResourceGroup

获取Bucket的资源组ID。

自定义域名（CNAME）

CreateCnameToken

创建域名所有权验证所需的CnameToken。

GetCnameToken

获取已创建的CnameToken。

PutCname

为某个Bucket绑定CNAME。

ListCname

获取某个Bucket下绑定的所有CNAME列表。

DeleteCname

删除已绑定的CNAME。

图片样式（Style）

PutStyle

新增图片样式。

GetStyle

获取某个Bucket下指定的图片样式信息。

ListStyle

获取某个Bucket下已创建的所有图片样式。

DeleteStyle

删除某个Bucket下指定的图片样式。

安全传输层协议（TLS）

PutBucketHttpsConfig

为Bucket开启或关闭TLS版本设置。

GetBucketHttpsConfig

查看Bucket的TLS版本设置。

存储冗余转换（RedundancyTransition）

CreateBucketDataRedundancyTransition

创建存储冗余转换任务。

GetBucketDataRedundancyTransition

获取存储冗余转换任务。

DeleteBucketDataRedundancyTransition

删除存储冗余转换任务。

ListUserDataRedundancyTransition

列举请求者所有的存储冗余转换任务。

ListBucketDataRedundancyTransition

列举某个Bucket下所有的存储冗余转换任务。

接入点（AccessPoint）

CreateAccessPoint

创建接入点。

GetAccessPoint

获取接入点信息。

DeleteAccessPoint

删除接入点。

ListAccessPoints

获取用户级别或Bucket级别的接入点信息。

PutAccessPointPolicy

配置接入点策略。

GetAccessPointPolicy

获取接入点策略配置。

DeleteAccessPointPolicy

删除接入点策略。

对象FC接入点（Object FC AccessPoint）

CreateAccessPointForObjectProcess

创建对象FC接入点。

GetAccessPointForObjectProcess

获取对象FC接入点基础信息。

DeleteAccessPointForObjectProcess

删除对象FC接入点。

ListAccessPointsForObjectProcess

获取用户级别的对象FC接入点信息。

PutAccessPointConfigForObjectProcess

修改对象FC接入点配置。

GetAccessPointConfigForObjectProcess

获取对象FC接入点配置信息。

PutAccessPointPolicyForObjectProcess

为对象FC接入点配置权限策略。

GetAccessPointPolicyForObjectProcess

获取对象FC接入点的权限策略配置。

DeleteAccessPointPolicyForObjectProcess

删除对象FC接入点的权限策略。

WriteGetObjectResponse

自定义返回数据和响应标头。

阻止公共访问（BlockAccess）

PutPublicAccessBlock

为OSS全局开启阻止公共访问。

GetPublicAccessBlock

获取OSS全局阻止公共访问的配置信息。

DeletePublicAccessBlock

删除OSS全局阻止公共访问配置信息。

PutBucketPublicAccessBlock

为Bucket开启阻止公共访问。

GetBucketPublicAccessBlock

获取指定Bucket的阻止公共访问配置信息。

DeleteBucketPublicAccessBlock

删除指定Bucket的阻止公共访问配置信息。

PutAccessPointPublicAccessBlock

为接入点开启阻止公共访问。

GetAccessPointPublicAccessBlock

获取指定接入点的阻止公共访问配置信息。

DeleteAccessPointPublicAccessBlock

删除指定接入点的阻止公共访问配置信息。

归档直读（ArchiveDirectRead）

PutBucketArchiveDirectRead

开启或关闭归档直读。

GetBucketArchiveDirectRead

查看是否已开启归档直读。

OSS加速器（OSS Accelerator）

PutBucketDataAccelerator

创建OSS加速器或修改其配置。

GetBucketDataAccelerator

查询OSS加速器信息。

DeleteBucketDataAccelerator

删除OSS加速器。

关于Object的操作
放大查看
分类

APi

描述

基础操作

PutObject

上传Object。

GetObject

获取Object。

CopyObject

拷贝Object。

AppendObject

以追加写的方式上传Object。

SealAppendObject

禁止向一个Appendable Object继续追加内容。

DeleteObject

删除单个Object。

DeleteMultipleObjects

删除多个Object。

HeadObject

只返回某个Object的meta信息，不返回文件内容。

GetObjectMeta

返回Object的基本meta信息，包括该Object的ETag、Size（文件大小）以及LastModified等，不返回文件内容。

PostObject

通过HTML表单上传的方式上传Object。

Callback

您只需在发送给OSS的请求中携带相应的Callback参数即可实现回调。

RestoreObject

解冻归档存储、冷归档存储或者深度冷归档存储类型的Object。

CleanRestoredObject

提前结束解冻状态。

SelectObject

对目标文件执行SQL语句，返回执行结果。

CreateSelectObjectMeta

获取目标文件总行数、总列数（对于CSV文件），以及Splits个数。

分片上传（MultipartUpload）

InitiateMultipartUpload

初始化一个Multipart Upload事件。

UploadPart

根据指定的Object名和uploadId来分块（Part）上传数据。

UploadPartCopy

通过在UploadPart请求的基础上增加一个请求头x-oss-copy-source来调用UploadPartCopy接口，实现从一个已存在的Object中拷贝数据来上传一个Part。

CompleteMultipartUpload

在将所有数据Part都上传完成后，您必须调用CompleteMultipartUpload接口来完成整个文件的分片上传。

AbortMultipartUpload

取消Multipart Upload事件并删除对应的Part数据。

ListMultipartUploads

列举所有执行中的Multipart Upload事件，即已经初始化但还未完成（Complete）或者还未中止（Abort）的Multipart Upload事件。

ListParts

列举指定uploadId所属的所有已经上传成功Part。

权限控制（ACL)

PutObjectACL

修改Object的访问权限。

GetObjectACL

查看Object的访问权限。

软链接（Symlink）

PutSymlink

创建软链接。

GetSymlink

获取软链接。

标签（Tagging）

PutObjectTagging

设置或更新对象标签。

GetObjectTagging

获取对象标签信息。

DeleteObjectTagging

删除指定的对象标签。

关于向量Bucket的操作
放大查看
分类

APi

描述

向量 Bucket

PutVectorBucket

创建向量 Bucket。

GetVectorBucket

获取向量Bucket的详细信息。

ListVectorBuckets

列举当前账号下的所有向量Bucket。

DeleteVectorBucket

删除向量Bucket。

索引 Index

PutVectorIndex

在向量 Bucket中创建向量索引。

GetVectorIndex

获取向量索引的详细信息。

ListVectorIndexes

列举向量Bucket中的所有向量索引。

DeleteVectorIndex

在将所有数据Part都上传完成后，您必须调用CompleteMultipartUpload接口来完成整个文件的分片上传。

向量 Vectors

PutVectors

向索引中写入向量数据。

GetVectors

获取指定的向量数据。

ListVectors

列举向量索引中的所有向量数据。

DeleteVectors

删除向量索引中的指定向量数据。

QueryVectors

进行向量相似性检索。

关于资源组QoS的操作
放大查看
API

描述

PutBucketQoSInfo

为资源池内的Bucket设置流控。

GetBucketQoSInfo

获取资源池内某个Bucket的流控配置。

DeleteBucketQosInfo

删除资源池内指定Bucket的流控配置。

PutBucketRequesterQoSInfo

设置请求者在Bucket级别的流控。

GetBucketRequesterQoSInfo

获取指定请求者在Bucket级别的流控配置。

ListBucketRequesterQoSInfos

获取所有请求者在Bucket级别的流控配置。

DeleteBucketRequesterQoSInfo

删除Bucket的某个请求者流控配置。

ListResourcePools

获取当前账号下的所有资源池信息。

GetResourcePoolInfo

获取指定资源池流控配置。

ListResourcePoolBuckets

获取指定资源池包含的Bucket列表。

PutResourcePoolRequesterQoSInfo

为资源池的请求者配置流控。

GetResourcePoolRequesterQoSInfo

获取指定请求者在资源池的流控配置。

ListResourcePoolRequesterQoSInfos

获取所有请求者在资源池的流控配置。

DeleteResourcePoolRequesterQoSInfo

删除指定请求者在资源池的流控配置。

PutBucketResourcePoolBucketGroup

将资源池内的Bucket添加到BucketGroup。

ListResourcePoolBucketGroups

列举指定资源池的BucketGroup列表。

PutResourcePoolBucketGroupQoSInfo

配置或修改资源池中BucketGroup的流控。

GetResourcePoolBucketGroupQoSInfo

获取资源池中BucketGroup的流控配置。

ListResourcePoolBucketGroupQoSInfos

列举资源池中BucketGroup的流控配置。

DeleteResourcePoolBucketGroupQoSInfo

删除资源池中BucketGroup的流控配置。

关于Live Channel的操作
放大查看
API

描述

PutLiveChannelStatus

切换LiveChannel的状态。

PutLiveChannel

创建LiveChannel。

GetVodPlaylist

获取播放列表。

PostVodPlaylist

生成播放列表。

GetLiveChannelStat

获取LiveChannel的推流状态信息。

GetLiveChannelInfo

获取LiveChannel的配置信息。

GetLiveChannelHistory

获取LiveChannel的推流记录。

ListLiveChannel

列举LiveChannel。

DeleteLiveChannel

删除LiveChannel。

附录
公共HTTP头定义

OSS错误码

关于Service操作

关于Region操作

关于Bucket操作

关于Object操作

关于向量Bucket操作

关于资源池QoS操作

关于LiveChannel的操作

以上内容是否对您有帮助？

有帮助

没帮助
如需咨询产品相关问题，请 点此反