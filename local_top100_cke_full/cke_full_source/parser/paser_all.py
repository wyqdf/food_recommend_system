import argparse
from abc import ABC, abstractmethod


class BaseArgs(ABC):
    """推荐系统模型参数解析基类"""

    def __init__(self, model_name: str):
        self.model_name = model_name
        self.parser = argparse.ArgumentParser(description=f"Run {model_name}.")
        self._add_common_args()
        self._add_model_specific_args()

    def _add_common_args(self):
        """添加所有模型共同的参数"""
        # 基础设置
        self.parser.add_argument('--seed', type=int, default=2019,
                                 help='Random seed.')

        # 数据相关
        self.parser.add_argument('--data_name', nargs='?', default='amazon-book',
                                 help='Choose a dataset from {yelp2018, last-fm, amazon-book}')
        self.parser.add_argument('--data_dir', nargs='?', default='datasets/',
                                 help='Input data path.')

        # 预训练相关
        self.parser.add_argument('--use_pretrain', type=int, default=0,
                                 help='0: No pretrain, 1: Pretrain with the learned embeddings, 2: Pretrain with stored model.')
        self.parser.add_argument('--pretrain_embedding_dir', nargs='?', default='datasets/pretrain/',
                                 help='Path of learned embeddings.')
        self.parser.add_argument('--pretrain_model_path', nargs='?', default='trained_model/model.pth',
                                 help='Path of stored model.')

        # 嵌入维度
        self.parser.add_argument('--embed_dim', type=int, default=64,
                                 help='User / entity Embedding size.')

        # 批处理大小
        self.parser.add_argument('--test_batch_size', type=int, default=4096,
                                 help='Test batch size (the number of users to test every batch).')

        # 训练参数
        self.parser.add_argument('--lr', type=float, default=0.0003,
                                 help='Learning rate.')
        self.parser.add_argument('--n_epoch', type=int, default=1000,
                                 help='Number of epoch.')
        self.parser.add_argument('--stopping_steps', type=int, default=10,
                                 help='Number of epoch for early stopping')

        # 打印和评估
        self.parser.add_argument('--print_every', type=int, default=10,
                                 help='Iter interval of printing loss.')
        self.parser.add_argument('--evaluate_every', type=int, default=10,
                                 help='Epoch interval of evaluating CF.')

        # 评估指标
        self.parser.add_argument('--Ks', nargs='?', default='[20, 40, 60, 80, 100]',
                                 help='Calculate metric@K when evaluating.')

    @abstractmethod
    def _add_model_specific_args(self):
        """添加模型特定的参数，子类必须实现"""
        pass

    @abstractmethod
    def _get_save_dir_format(self):
        """返回保存目录的格式字符串，子类必须实现"""
        pass

    def parse_args(self):
        """解析参数并设置保存目录"""
        args = self.parser.parse_args()
        args.save_dir = self._get_save_dir_format().format(**vars(args))
        return args


class BPRMFArgs(BaseArgs):
    """BPRMF模型参数解析器"""

    def __init__(self):
        super().__init__("BPRMF")

    def _add_model_specific_args(self):
        # L2正则化
        self.parser.add_argument('--l2loss_lambda', type=float, default=1e-5,
                                 help='Lambda when calculating CF l2 loss.')

        # 批处理大小
        self.parser.add_argument('--train_batch_size', type=int, default=1024,
                                 help='Train batch size.')

    def _get_save_dir_format(self):
        return 'trained_model/BPRMF/{data_name}/embed-dim{embed_dim}_lr{lr}_pretrain{use_pretrain}/'


class CKEArgs(BaseArgs):
    """CKE模型参数解析器"""

    def __init__(self):
        super().__init__("CKE")

    def _add_model_specific_args(self):
        # 批处理大小
        self.parser.add_argument('--cf_batch_size', type=int, default=1024,
                                 help='CF batch size.')
        self.parser.add_argument('--kg_batch_size', type=int, default=2048,
                                 help='KG batch size.')

        # 关系维度
        self.parser.add_argument('--relation_dim', type=int, default=64,
                                 help='Relation Embedding size.')

        # L2正则化
        self.parser.add_argument('--kg_l2loss_lambda', type=float, default=1e-5,
                                 help='Lambda when calculating KG l2 loss.')
        self.parser.add_argument('--cf_l2loss_lambda', type=float, default=1e-5,
                                 help='Lambda when calculating CF l2 loss.')

        self.parser.set_defaults(test_batch_size = 256)

    def _get_save_dir_format(self):
        return 'trained_model/CKE/{data_name}/embed-dim{embed_dim}_relation-dim{relation_dim}_lr{lr}_pretrain{use_pretrain}/'


class ECFKGArgs(BaseArgs):
    """ECFKG模型参数解析器"""

    def __init__(self):
        super().__init__("ECFKG")

    def _add_model_specific_args(self):
        # 批处理大小
        self.parser.add_argument('--train_batch_size', type=int, default=2048,
                                 help='KG batch size.')
        self.parser.set_defaults(lr = 0.0001)

    def _get_save_dir_format(self):
        return 'trained_model/ECFKG/{data_name}/embed-dim{embed_dim}_lr{lr}_pretrain{use_pretrain}/'


class KGATArgs(BaseArgs):
    """KGAT模型参数解析器"""

    def __init__(self):
        super().__init__("KGAT")

    def _add_model_specific_args(self):
        # 重写默认数据集和预训练设置
        self.parser.set_defaults(data_name='meishitianxia',embed_dim=128, n_epoch=10000)

        # 批处理大小
        self.parser.add_argument('--cf_batch_size', type=int, default=2048,
                                 help='CF batch size.')
        self.parser.add_argument('--kg_batch_size', type=int, default=2048,
                                 help='KG batch size.')

        # 关系维度
        self.parser.add_argument('--relation_dim', type=int, default=128,
                                 help='Relation Embedding size.')

        # KGAT特有参数
        self.parser.add_argument('--laplacian_type', type=str, default='random-walk',
                                 help='Specify the type of the adjacency (laplacian) matrix from {symmetric, random-walk}.')
        self.parser.add_argument('--aggregation_type', type=str, default='bi-interaction',
                                 help='Specify the type of the aggregation layer from {gcn, graphsage, bi-interaction}.')
        self.parser.add_argument('--conv_dim_list', nargs='?', default='[64, 32, 16]',
                                 help='Output sizes of every aggregation layer.')
        self.parser.add_argument('--mess_dropout', nargs='?', default='[0.1, 0.1, 0.1]',
                                 help='Dropout probability w.r.t. message dropout for each deep layer. 0: no dropout.')

        # L2正则化
        self.parser.add_argument('--kg_l2loss_lambda', type=float, default=1e-5,
                                 help='Lambda when calculating KG l2 loss.')
        self.parser.add_argument('--cf_l2loss_lambda', type=float, default=1e-5,
                                 help='Lambda when calculating CF l2 loss.')

        # 特定打印参数
        self.parser.add_argument('--cf_print_every', type=int, default=1,
                                 help='Iter interval of printing CF loss.')
        self.parser.add_argument('--kg_print_every', type=int, default=1,
                                 help='Iter interval of printing KG loss.')

    def _get_save_dir_format(self):
        def format_save_dir(args):
            conv_dims = '-'.join([str(i) for i in eval(args.conv_dim_list)])
            return f'trained_model/KGAT/{args.data_name}/embed-dim{args.embed_dim}_relation-dim{args.relation_dim}_{args.laplacian_type}_{args.aggregation_type}_{conv_dims}_lr{args.lr}_pretrain{args.use_pretrain}/'

        # 返回一个特殊的格式化函数
        return format_save_dir

    def parse_args(self):
        """重写解析方法以处理特殊的保存目录格式"""
        args = self.parser.parse_args()
        format_func = self._get_save_dir_format()
        args.save_dir = format_func(args)
        return args


class NFMArgs(BaseArgs):
    """NFM模型参数解析器"""

    def __init__(self):
        super().__init__("NFM")

    def _add_model_specific_args(self):
        # 重写评估间隔默认值
        self.parser.set_defaults(evaluate_every=50)

        # 模型类型
        self.parser.add_argument('--model_type', nargs='?', default='nfm',
                                 help='Specify a model type from {fm, nfm}.')

        # 网络结构
        self.parser.add_argument('--hidden_dim_list', nargs='?', default='[64, 32, 16]',
                                 help='Output sizes of every hidden layer.')
        self.parser.add_argument('--mess_dropout', nargs='?', default='[0.1, 0.1, 0.1]',
                                 help='Dropout probability w.r.t. message dropout for bi-interaction layer and each hidden layer. 0: no dropout.')

        # L2正则化
        self.parser.add_argument('--l2loss_lambda', type=float, default=1e-5,
                                 help='Lambda when calculating l2 loss.')

        # 批处理大小
        self.parser.add_argument('--train_batch_size', type=int, default=1024,
                                 help='Train batch size.')
        # 重新定义test_batch_size为更小的值
        self.parser.set_defaults(test_batch_size = 32)

        # 测试核心数
        self.parser.add_argument('--test_cores', type=int, default=8,
                                 help='Number of cores when evaluating.')

    def _get_save_dir_format(self):
        def format_save_dir(args):
            hidden_dims = '-'.join([str(i) for i in eval(args.hidden_dim_list)])
            return f'trained_model/NFM/{args.data_name}/{args.model_type}_embed-dim{args.embed_dim}_{hidden_dims}_lr{args.lr}_pretrain{args.use_pretrain}/'

        return format_save_dir

    def parse_args(self):
        """重写解析方法以处理特殊的保存目录格式"""
        args = self.parser.parse_args()
        format_func = self._get_save_dir_format()
        args.save_dir = format_func(args)
        return args
class MMKGATArgs(BaseArgs):
    """多模态KGAT模型 (MultiModalKGAT) 参数解析器"""

    def __init__(self):
        super().__init__("MMKGAT")

    def _add_model_specific_args(self):
        # 覆写默认数据集和一些基础设置
        self.parser.set_defaults(data_name='meishitianxia', embed_dim=64, n_epoch=1000)

        # ------------------
        # 1. 批处理大小
        # ------------------
        # 注意：由于涉及图像读取，多模态的 CF batch size 不宜设置得像纯文本那么大，否则容易爆显存
        self.parser.add_argument('--cf_batch_size', type=int, default=1024,
                                 help='CF batch size (Multi-modal).')
        self.parser.add_argument('--kg_batch_size', type=int, default=2048,
                                 help='KG batch size.')

        # ------------------
        # 2. 多模态专属参数 (New)
        # ------------------
        self.parser.add_argument('--vocab_size', type=int, default=10000,
                                 help='Vocabulary size for text encoder.')
        self.parser.add_argument('--max_text_len', type=int, default=50,
                                 help='Maximum length of text sequence.')
        self.parser.add_argument('--img_size', type=int, default=64,
                                 help='Image resize dimension (e.g., 64 means 64x64).')

        # ------------------
        # 3. KGAT 图网络相关参数
        # ------------------
        self.parser.add_argument('--relation_dim', type=int, default=64,
                                 help='Relation Embedding size.')
        self.parser.add_argument('--laplacian_type', type=str, default='random-walk',
                                 help='Specify the type of the adjacency (laplacian) matrix from {symmetric, random-walk}.')
        self.parser.add_argument('--aggregation_type', type=str, default='bi-interaction',
                                 help='Specify the type of the aggregation layer from {gcn, graphsage, bi-interaction}.')
        self.parser.add_argument('--conv_dim_list', nargs='?', default='[64, 32, 16]',
                                 help='Output sizes of every aggregation layer.')
        self.parser.add_argument('--mess_dropout', nargs='?', default='[0.1, 0.1, 0.1]',
                                 help='Dropout probability w.r.t. message dropout for each deep layer. 0: no dropout.')

        # ------------------
        # 4. L2 正则化与打印
        # ------------------
        self.parser.add_argument('--kg_l2loss_lambda', type=float, default=1e-5,
                                 help='Lambda when calculating KG l2 loss.')
        self.parser.add_argument('--cf_l2loss_lambda', type=float, default=1e-5,
                                 help='Lambda when calculating CF l2 loss.')
        self.parser.add_argument('--cf_print_every', type=int, default=1,
                                 help='Iter interval of printing CF loss.')
        self.parser.add_argument('--kg_print_every', type=int, default=1,
                                 help='Iter interval of printing KG loss.')

    def _get_save_dir_format(self):
        def format_save_dir(args):
            conv_dims = '-'.join([str(i) for i in eval(args.conv_dim_list)])
            # 保存路径中加入多模态的标识 (MM)
            return f'trained_model/MMKGAT/{args.data_name}/embed-dim{args.embed_dim}_{args.aggregation_type}_{conv_dims}_lr{args.lr}/'

        return format_save_dir

    def parse_args(self):
        """重写解析方法以处理特殊的保存目录格式"""
        args = self.parser.parse_args()
        format_func = self._get_save_dir_format()
        args.save_dir = format_func(args)
        return args


class CKEFullArgs(BaseArgs):
    """
    CKEFull 模型参数解析器 —— 在 CKE 基础上加入 SDAE (文本) + SCAE (视觉) 多模态分支。
    数据集默认指向 meishitianxia_v1。
    """

    def __init__(self):
        super().__init__("CKEFull")

    def _add_model_specific_args(self):
        # ── 数据集默认值 ──────────────────────────────────────────────────────
        self.parser.set_defaults(
            data_name='meishitianxia_v1',
            embed_dim=64,
            n_epoch=100,
            lr=0.001,
            test_batch_size=128,
            evaluate_every=5,
            stopping_steps=10,
            print_every=50,
        )

        # ── 批处理大小 ─────────────────────────────────────────────────────────
        self.parser.add_argument('--cf_batch_size', type=int, default=256,
                                 help='CF batch size.')
        self.parser.add_argument('--kg_batch_size', type=int, default=256,
                                 help='KG batch size.')
        self.parser.add_argument('--sdae_batch_size', type=int, default=256,
                                 help='SDAE (text autoencoder) batch size.')
        self.parser.add_argument('--scae_batch_size', type=int, default=32,
                                 help='SCAE (image autoencoder) batch size.')

        # ── 关系嵌入 ───────────────────────────────────────────────────────────
        self.parser.add_argument('--relation_dim', type=int, default=32,
                                 help='Relation embedding size.')

        # ── 文本 (SDAE) ────────────────────────────────────────────────────────
        self.parser.add_argument('--n_vocab', type=int, default=2000,
                                 help='Max vocabulary size for bag-of-chars text features.')
        self.parser.add_argument('--sdae_dim_list', type=str, default='[600, 200]',
                                 help='Hidden layer sizes of the SDAE encoder (JSON list).')
        self.parser.add_argument('--sdae_dropout', type=float, default=0.2,
                                 help='Masking (dropout) fraction applied to SDAE input.')

        # ── 视觉 (SCAE) ────────────────────────────────────────────────────────
        self.parser.add_argument('--image_height', type=int, default=64,
                                 help='Image height in pixels (images are pre-resized to 64×64).')
        self.parser.add_argument('--image_width', type=int, default=64,
                                 help='Image width in pixels.')
        self.parser.add_argument('--scae_channel_list', type=str, default='[8,16]',
                                 help='Conv channel sizes of the SCAE encoder (JSON list).')
        self.parser.add_argument('--scae_kernel_list', type=str, default='[5, 3]',
                                 help='Conv kernel sizes of the SCAE encoder (JSON list).')
        self.parser.add_argument('--scae_dropout', type=float, default=0.2,
                                 help='Masking fraction applied to SCAE input.')

        # ── 正则化 ─────────────────────────────────────────────────────────────
        self.parser.add_argument('--kg_l2loss_lambda', type=float, default=1e-5,
                                 help='L2 regularisation weight for KG loss.')
        self.parser.add_argument('--cf_l2loss_lambda', type=float, default=1e-5,
                                 help='L2 regularisation weight for CF loss.')

    def _get_save_dir_format(self):
        # uses a callable so we can embed sdae/scae info cleanly
        def format_save_dir(args):
            return (
                f'trained_model/CKEFull/{args.data_name}/'
                f'embed-dim{args.embed_dim}_relation-dim{args.relation_dim}'
                f'_vocab{args.n_vocab}_lr{args.lr}_pretrain{args.use_pretrain}/'
            )
        return format_save_dir

    def parse_args(self):
        args = self.parser.parse_args()
        # parse list arguments from JSON strings
        args.sdae_dim_list     = eval(args.sdae_dim_list)
        args.scae_channel_list = eval(args.scae_channel_list)
        args.scae_kernel_list  = eval(args.scae_kernel_list)
        format_func = self._get_save_dir_format()
        args.save_dir = format_func(args)
        return args


class CKEFullModifyArgs(BaseArgs):
    """
    CKEFullModify 模型参数解析器
    在 CKEFull 的基础上增加了 review_align_weight (评论一致性对齐权重)
    """

    def __init__(self):
        super().__init__("CKEFullModify")

    def _add_model_specific_args(self):
        # ── 数据集默认值 ──────────────────────────────────────────────────────
        self.parser.set_defaults(
            data_name='meishitianxia_v1',
            embed_dim=64,
            n_epoch=1000,
            lr=0.0001,
            test_batch_size=128,
            evaluate_every=5,
            stopping_steps=10,
            print_every=50,
        )

        # ── 批处理大小 ─────────────────────────────────────────────────────────
        self.parser.add_argument('--cf_batch_size', type=int, default=256,
                                 help='CF batch size.')
        self.parser.add_argument('--kg_batch_size', type=int, default=256,
                                 help='KG batch size.')
        self.parser.add_argument('--sdae_batch_size', type=int, default=256,
                                 help='SDAE (text/review autoencoder) batch size.')
        self.parser.add_argument('--scae_batch_size', type=int, default=32,
                                 help='SCAE (image autoencoder) batch size.')

        # ── 关系嵌入 ───────────────────────────────────────────────────────────
        self.parser.add_argument('--relation_dim', type=int, default=32,
                                 help='Relation embedding size.')

        # ── 文本与评论 (SDAE & Review) ─────────────────────────────────────────
        self.parser.add_argument('--n_vocab', type=int, default=2000,
                                 help='Max vocabulary size for text features.')
        self.parser.add_argument('--sdae_dim_list', type=str, default='[600, 200]',
                                 help='Hidden layer sizes of the SDAE encoder (JSON list).')
        self.parser.add_argument('--sdae_dropout', type=float, default=0.2,
                                 help='Masking (dropout) fraction applied to SDAE input.')

        # 【新增】对比学习的对齐权重
        self.parser.add_argument('--review_align_weight', type=float, default=0.5,
                                 help='Weight for the bilinear review consistency alignment loss.')

        # ── 视觉 (SCAE) ────────────────────────────────────────────────────────
        self.parser.add_argument('--image_height', type=int, default=64,
                                 help='Image height in pixels.')
        self.parser.add_argument('--image_width', type=int, default=64,
                                 help='Image width in pixels.')
        self.parser.add_argument('--scae_channel_list', type=str, default='[8,16]',
                                 help='Conv channel sizes of the SCAE encoder (JSON list).')
        self.parser.add_argument('--scae_kernel_list', type=str, default='[5, 3]',
                                 help='Conv kernel sizes of the SCAE encoder (JSON list).')
        self.parser.add_argument('--scae_dropout', type=float, default=0.2,
                                 help='Masking fraction applied to SCAE input.')

        # ── 正则化 ─────────────────────────────────────────────────────────────
        self.parser.add_argument('--kg_l2loss_lambda', type=float, default=1e-5,
                                 help='L2 regularisation weight for KG loss.')
        self.parser.add_argument('--cf_l2loss_lambda', type=float, default=1e-5,
                                 help='L2 regularisation weight for CF loss.')

    def _get_save_dir_format(self):
        # 【修改】保存路径变为 CKEFullModify，并将 align 权重写入目录名，防止不同参数的权重互相覆盖
        def format_save_dir(args):
            return (
                f'trained_model/CKEFullModify/{args.data_name}/'
                f'embed{args.embed_dim}_rel{args.relation_dim}'
                f'_align{args.review_align_weight}_lr{args.lr}_pretrain{args.use_pretrain}/'
            )

        return format_save_dir

    def parse_args(self):
        args = self.parser.parse_args()
        args.sdae_dim_list = eval(args.sdae_dim_list)
        args.scae_channel_list = eval(args.scae_channel_list)
        args.scae_kernel_list = eval(args.scae_kernel_list)
        format_func = self._get_save_dir_format()
        args.save_dir = format_func(args)
        return args


# 【新增】便捷函数
def parse_cke_full_modify_args():
    return CKEFullModifyArgs().parse_args()

# 便捷函数
def parse_bprmf_args():
    return BPRMFArgs().parse_args()


def parse_cke_args():
    return CKEArgs().parse_args()


def parse_ecfkg_args():
    return ECFKGArgs().parse_args()


def parse_kgat_args():
    return KGATArgs().parse_args()


def parse_nfm_args():
    return NFMArgs().parse_args()

def parse_mmkgat_args():
    return MMKGATArgs().parse_args()

def parse_cke_full_args():
    return CKEFullArgs().parse_args()