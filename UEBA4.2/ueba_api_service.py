"""
UEBA异常检测API服务
提供用户级别和事件级别的双层异常检测接口
供Java客户端调用

功能：
1. 用户级别异常检测 - 基于聚合特征判断用户整体是否异常
2. 事件级别异常检测 - 判断具体行为事件（登录、文件操作等）是否异常
3. 综合检测 - 同时返回用户级别和事件级别的异常分数

使用：
python ueba_api_service.py --model result/incremental_ocsvm_model.joblib --host 0.0.0.0 --port 5004
"""

from flask import Flask, request, jsonify
import pandas as pd
import numpy as np
import joblib
import os
import sys
from datetime import datetime
import traceback

# 添加model目录到Python路径
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'model'))

app = Flask(__name__)

# 全局变量
detector = None
model_data = None

class UnifiedDetectionSystem:
    """统一检测系统 - 支持用户级别和事件级别检测"""
    
    def __init__(self, model_path):
        """加载模型"""
        print(f"加载模型: {model_path}")
        self.model_data = joblib.load(model_path)
        self.model = self.model_data['model']
        self.scaler = self.model_data['scaler']
        self.feature_columns = self.model_data['feature_columns']
        print(f"✓ 模型加载成功，特征维度: {len(self.feature_columns)}")
    
    def comprehensive_detection(self, user_id, behavior_data):
        """
        综合检测：同时返回用户级别和事件级别的异常分数
        
        参数:
            user_id: 用户ID
            behavior_data: 用户行为数据
                {
                    'logon_records': [...],
                    'device_records': [...],
                    'file_records': [...],
                    'http_records': [...],
                    'email_records': [...]
                }
        """
        result = {
            'user': user_id,
            'user_level': None,
            'event_level': {},
            'summary': {},
            'timestamp': datetime.now().isoformat()
        }
        
        # 1. 用户级别检测
        user_level_result = self._detect_user_level(user_id, behavior_data)
        result['user_level'] = user_level_result
        
        # 2. 事件级别检测
        event_level_result = self._detect_event_level(user_id, behavior_data)
        result['event_level'] = event_level_result
        
        # 3. 生成摘要
        result['summary'] = self._generate_summary(user_level_result, event_level_result)
        
        return result
    
    def _detect_user_level(self, user_id, behavior_data):
        """用户级别检测 - 提取聚合特征并预测"""
        features = {}
        
        # 提取各维度特征
        if behavior_data.get('logon_records'):
            features.update(self._extract_logon_features(behavior_data['logon_records']))
        
        if behavior_data.get('device_records'):
            features.update(self._extract_device_features(behavior_data['device_records']))
        
        if behavior_data.get('file_records'):
            features.update(self._extract_file_features(behavior_data['file_records']))
        
        if behavior_data.get('http_records'):
            features.update(self._extract_http_features(behavior_data['http_records']))
        
        if behavior_data.get('email_records'):
            features.update(self._extract_email_features(behavior_data['email_records']))
        
        # 构建特征向量
        feature_df = pd.DataFrame([features])
        for col in self.feature_columns:
            if col not in feature_df.columns:
                feature_df[col] = 0
        feature_df = feature_df[self.feature_columns]
        
        # 预测
        X_scaled = self.scaler.transform(feature_df.values)
        anomaly_score = float(self.model.score_samples(X_scaled)[0])
        
        # 分类风险（基于实验#10的阈值）
        risk_level, risk_percentile = self._classify_user_risk(anomaly_score)
        is_anomaly = risk_level != '正常'
        
        return {
            'user': user_id,
            'anomaly_score': anomaly_score,
            'risk_level': risk_level,
            'risk_percentile': risk_percentile,
            'is_anomaly': is_anomaly,
            'features_count': len([f for f in features.values() if f > 0])
        }
    
    def _detect_event_level(self, user_id, behavior_data):
        """事件级别检测 - 检测每个具体事件"""
        result = {
            'logon_events': [],
            'device_events': [],
            'file_events': [],
            'http_events': [],
            'email_events': []
        }
        
        # 先建立用户行为基线
        profile = self._build_user_profile(behavior_data)
        
        # 检测每个Logon事件
        if behavior_data.get('logon_records'):
            for record in behavior_data['logon_records']:
                event_result = self._detect_single_logon(record, profile.get('logon'))
                event_result['event'] = record
                result['logon_events'].append(event_result)
        
        # 检测每个Device事件
        if behavior_data.get('device_records'):
            for record in behavior_data['device_records']:
                event_result = self._detect_single_device(record, profile.get('device'))
                event_result['event'] = record
                result['device_events'].append(event_result)
        
        # 检测每个File事件
        if behavior_data.get('file_records'):
            for record in behavior_data['file_records']:
                event_result = self._detect_single_file(record, profile.get('file'))
                event_result['event'] = record
                result['file_events'].append(event_result)
        
        # 检测每个HTTP事件
        if behavior_data.get('http_records'):
            for record in behavior_data['http_records']:
                event_result = self._detect_single_http(record, profile.get('http'))
                event_result['event'] = record
                result['http_events'].append(event_result)
        
        # 检测每个Email事件
        if behavior_data.get('email_records'):
            for record in behavior_data['email_records']:
                event_result = self._detect_single_email(record, profile.get('email'))
                event_result['event'] = record
                result['email_events'].append(event_result)
        
        return result
    
    def _build_user_profile(self, behavior_data):
        """从行为数据建立用户基线"""
        profile = {}
        
        # Logon基线
        logon_records = behavior_data.get('logon_records', [])
        if len(logon_records) >= 5:
            df = pd.DataFrame(logon_records)
            df['date'] = pd.to_datetime(df['date'])
            df['hour'] = df['date'].dt.hour
            df['day_of_week'] = df['date'].dt.dayofweek
            
            profile['logon'] = {
                'typical_hours': df['hour'].value_counts().head(5).index.tolist(),
                'typical_pcs': df['pc'].value_counts().head(3).index.tolist() if 'pc' in df.columns else [],
                'avg_hour': float(df['hour'].mean()),
                'night_ratio': float(((df['hour'] >= 22) | (df['hour'] < 6)).mean()),
                'weekend_ratio': float(df['day_of_week'].isin([5, 6]).mean())
            }
        
        # Device基线
        device_records = behavior_data.get('device_records', [])
        if len(device_records) >= 3:
            df = pd.DataFrame(device_records)
            df['date'] = pd.to_datetime(df['date'])
            df['hour'] = df['date'].dt.hour
            
            profile['device'] = {
                'typical_hours': df['hour'].value_counts().head(5).index.tolist(),
                'avg_hour': float(df['hour'].mean())
            }
        
        # File基线
        file_records = behavior_data.get('file_records', [])
        if len(file_records) >= 3:
            df = pd.DataFrame(file_records)
            df['date'] = pd.to_datetime(df['date'])
            df['hour'] = df['date'].dt.hour
            
            profile['file'] = {
                'typical_hours': df['hour'].value_counts().head(5).index.tolist(),
                'avg_hour': float(df['hour'].mean())
            }
            
            if 'filename' in df.columns:
                df['extension'] = df['filename'].str.split('.').str[-1].str.lower()
                profile['file']['typical_extensions'] = df['extension'].value_counts().head(10).index.tolist()
        
        # HTTP基线
        http_records = behavior_data.get('http_records', [])
        if len(http_records) >= 5:
            df = pd.DataFrame(http_records)
            df['date'] = pd.to_datetime(df['date'])
            df['hour'] = df['date'].dt.hour
            
            profile['http'] = {
                'typical_hours': df['hour'].value_counts().head(5).index.tolist(),
                'avg_hour': float(df['hour'].mean())
            }
        
        # Email基线
        email_records = behavior_data.get('email_records', [])
        if len(email_records) >= 3:
            df = pd.DataFrame(email_records)
            df['date'] = pd.to_datetime(df['date'])
            df['hour'] = df['date'].dt.hour
            
            profile['email'] = {
                'typical_hours': df['hour'].value_counts().head(5).index.tolist(),
                'avg_hour': float(df['hour'].mean())
            }
            
            if 'size' in df.columns:
                profile['email']['avg_size'] = float(df['size'].mean())
                profile['email']['max_size'] = float(df['size'].quantile(0.95))
        
        return profile
    
    def _detect_single_logon(self, event, profile):
        """检测单次登录事件"""
        if not profile:
            return {'anomaly_score': 0.0, 'risk_level': '正常', 'anomalies': []}
        
        event_date = pd.to_datetime(event['date'])
        event_hour = event_date.hour
        event_day = event_date.dayofweek
        event_pc = event.get('pc', '')
        
        anomalies = []
        anomaly_score = 0.0
        
        # 检查时间异常
        if event_hour not in profile.get('typical_hours', []):
            anomalies.append({'type': '非典型时间', 'severity': 0.3})
            anomaly_score += 0.3
        
        # 深夜登录
        if (event_hour >= 22 or event_hour < 6) and profile.get('night_ratio', 0) < 0.1:
            anomalies.append({'type': '深夜登录', 'severity': 0.4})
            anomaly_score += 0.4
        
        # 周末异常
        if event_day in [5, 6] and profile.get('weekend_ratio', 0) < 0.1:
            anomalies.append({'type': '周末登录', 'severity': 0.2})
            anomaly_score += 0.2
        
        # PC异常
        if event_pc and event_pc not in profile.get('typical_pcs', []):
            anomalies.append({'type': '陌生设备', 'severity': 0.5})
            anomaly_score += 0.5
        
        anomaly_score = min(anomaly_score, 1.0)
        risk_level = self._classify_event_risk(anomaly_score)
        
        return {
            'anomaly_score': float(anomaly_score),
            'risk_level': risk_level,
            'anomalies': anomalies
        }
    
    def _detect_single_device(self, event, profile):
        """检测单次设备事件"""
        if not profile:
            return {'anomaly_score': 0.0, 'risk_level': '正常', 'anomalies': []}
        
        event_date = pd.to_datetime(event['date'])
        event_hour = event_date.hour
        
        anomalies = []
        anomaly_score = 0.0
        
        if event_hour not in profile.get('typical_hours', []):
            anomalies.append({'type': '非典型时间', 'severity': 0.3})
            anomaly_score += 0.3
        
        if event_hour >= 22 or event_hour < 6:
            anomalies.append({'type': '深夜设备使用', 'severity': 0.3})
            anomaly_score += 0.3
        
        anomaly_score = min(anomaly_score, 1.0)
        risk_level = self._classify_event_risk(anomaly_score)
        
        return {
            'anomaly_score': float(anomaly_score),
            'risk_level': risk_level,
            'anomalies': anomalies
        }
    
    def _detect_single_file(self, event, profile):
        """检测单次文件操作"""
        if not profile:
            return {'anomaly_score': 0.0, 'risk_level': '正常', 'anomalies': []}
        
        event_date = pd.to_datetime(event['date'])
        event_hour = event_date.hour
        filename = event.get('filename', '')
        extension = filename.split('.')[-1].lower() if '.' in filename else ''
        
        anomalies = []
        anomaly_score = 0.0
        
        if event_hour not in profile.get('typical_hours', []):
            anomalies.append({'type': '非典型时间', 'severity': 0.3})
            anomaly_score += 0.3
        
        if event_hour >= 22 or event_hour < 6:
            anomalies.append({'type': '深夜文件操作', 'severity': 0.4})
            anomaly_score += 0.4
        
        if extension and extension not in profile.get('typical_extensions', []):
            anomalies.append({'type': '罕见文件类型', 'severity': 0.3})
            anomaly_score += 0.3
        
        anomaly_score = min(anomaly_score, 1.0)
        risk_level = self._classify_event_risk(anomaly_score)
        
        return {
            'anomaly_score': float(anomaly_score),
            'risk_level': risk_level,
            'anomalies': anomalies
        }
    
    def _detect_single_http(self, event, profile):
        """检测单次HTTP访问"""
        if not profile:
            return {'anomaly_score': 0.0, 'risk_level': '正常', 'anomalies': []}
        
        event_date = pd.to_datetime(event['date'])
        event_hour = event_date.hour
        
        anomalies = []
        anomaly_score = 0.0
        
        if event_hour not in profile.get('typical_hours', []):
            anomalies.append({'type': '非典型时间', 'severity': 0.2})
            anomaly_score += 0.2
        
        if event_hour >= 22 or event_hour < 6:
            anomalies.append({'type': '深夜网络访问', 'severity': 0.3})
            anomaly_score += 0.3
        
        anomaly_score = min(anomaly_score, 1.0)
        risk_level = self._classify_event_risk(anomaly_score)
        
        return {
            'anomaly_score': float(anomaly_score),
            'risk_level': risk_level,
            'anomalies': anomalies
        }
    
    def _detect_single_email(self, event, profile):
        """检测单封邮件"""
        if not profile:
            return {'anomaly_score': 0.0, 'risk_level': '正常', 'anomalies': []}
        
        event_date = pd.to_datetime(event['date'])
        event_hour = event_date.hour
        email_size = event.get('size', 0)
        
        anomalies = []
        anomaly_score = 0.0
        
        if event_hour not in profile.get('typical_hours', []):
            anomalies.append({'type': '非典型时间', 'severity': 0.2})
            anomaly_score += 0.2
        
        if event_hour >= 22 or event_hour < 6:
            anomalies.append({'type': '深夜发送邮件', 'severity': 0.3})
            anomaly_score += 0.3
        
        if email_size > profile.get('max_size', float('inf')):
            anomalies.append({'type': '异常大邮件', 'severity': 0.5})
            anomaly_score += 0.5
        
        anomaly_score = min(anomaly_score, 1.0)
        risk_level = self._classify_event_risk(anomaly_score)
        
        return {
            'anomaly_score': float(anomaly_score),
            'risk_level': risk_level,
            'anomalies': anomalies
        }
    
    def _generate_summary(self, user_level, event_level):
        """生成检测摘要"""
        high_risk_events = []
        total_events = 0
        anomaly_events = 0
        
        for event_type, events in event_level.items():
            total_events += len(events)
            for event in events:
                if event['risk_level'] in ['高风险', '中风险']:
                    anomaly_events += 1
                    high_risk_events.append({
                        'type': event_type,
                        'event': event['event'],
                        'score': event['anomaly_score'],
                        'risk': event['risk_level'],
                        'reasons': event['anomalies']
                    })
        
        high_risk_events.sort(key=lambda x: x['score'], reverse=True)
        
        return {
            'user_anomaly_score': user_level['anomaly_score'],
            'user_risk_level': user_level['risk_level'],
            'total_events_analyzed': total_events,
            'anomaly_events_count': anomaly_events,
            'anomaly_rate': float(anomaly_events / total_events) if total_events > 0 else 0.0,
            'top_anomaly_events': high_risk_events[:10]
        }
    
    # ===== 特征提取函数 =====
    
    def _extract_logon_features(self, records):
        """提取Logon聚合特征"""
        if not records or len(records) < 5:
            return {}
        
        df = pd.DataFrame(records)
        df['date'] = pd.to_datetime(df['date'])
        df['hour'] = df['date'].dt.hour
        df['day_of_week'] = df['date'].dt.dayofweek
        
        features = {
            'logon_count': len(df),
            'logon_unique_pcs': df['pc'].nunique() if 'pc' in df.columns else 0,
            'logon_night_ratio': float(((df['hour'] >= 22) | (df['hour'] < 6)).mean()),
            'logon_weekend_ratio': float(df['day_of_week'].isin([5, 6]).mean()),
            'logon_pc_diversity': float(df['pc'].nunique() / max(len(df), 1)) if 'pc' in df.columns else 0.0,
            'logon_after_hours_ratio': float(((df['hour'] >= 18) | (df['hour'] < 9)).mean())
        }
        
        # 时间间隔
        time_diffs = df['date'].diff().dt.total_seconds() / 3600
        time_diffs = time_diffs[time_diffs > 0]
        if len(time_diffs) > 0:
            features['logon_avg_interval'] = float(time_diffs.mean())
            features['logon_std_interval'] = float(time_diffs.std())
        
        # PC使用
        if 'pc' in df.columns:
            pc_counts = df['pc'].value_counts()
            features['logon_max_pc_usage'] = float(pc_counts.max() / len(df)) if len(pc_counts) > 0 else 0.0
        
        return features
    
    def _extract_device_features(self, records):
        """提取Device聚合特征"""
        if not records or len(records) < 3:
            return {}
        
        df = pd.DataFrame(records)
        df['date'] = pd.to_datetime(df['date'])
        df['hour'] = df['date'].dt.hour
        
        features = {
            'device_events': len(df),
            'device_night_ratio': float(((df['hour'] >= 22) | (df['hour'] < 6)).mean()),
            'device_after_hours': float(((df['hour'] >= 18) | (df['hour'] < 9)).mean())
        }
        
        if 'activity' in df.columns:
            features['device_connects'] = int((df['activity'] == 'Connect').sum())
        
        if 'pc' in df.columns:
            features['device_pc_count'] = df['pc'].nunique()
        
        # 连接间隔
        if 'activity' in df.columns:
            connect_data = df[df['activity'] == 'Connect']
            if len(connect_data) > 1:
                time_diffs = connect_data['date'].diff().dt.total_seconds() / 3600
                time_diffs = time_diffs[time_diffs > 0]
                if len(time_diffs) > 0:
                    features['device_avg_interval'] = float(time_diffs.mean())
                    features['device_burst_ratio'] = float((time_diffs < 1).mean())
        
        return features
    
    def _extract_file_features(self, records):
        """提取File聚合特征"""
        if not records or len(records) < 3:
            return {}
        
        df = pd.DataFrame(records)
        df['date'] = pd.to_datetime(df['date'])
        df['hour'] = df['date'].dt.hour
        
        features = {
            'file_operations': len(df),
            'file_night_ratio': float(((df['hour'] >= 22) | (df['hour'] < 6)).mean()),
            'file_ops_per_day': float(len(df) / max(df['date'].dt.date.nunique(), 1))
        }
        
        if 'filename' in df.columns:
            features['file_unique_files'] = df['filename'].nunique()
            df['extension'] = df['filename'].str.split('.').str[-1]
            features['file_unique_extensions'] = df['extension'].nunique()
        
        if 'pc' in df.columns:
            features['file_pc_count'] = df['pc'].nunique()
        
        return features
    
    def _extract_http_features(self, records):
        """提取HTTP聚合特征"""
        if not records or len(records) < 3:
            return {}
        
        df = pd.DataFrame(records)
        df['date'] = pd.to_datetime(df['date'])
        df['hour'] = df['date'].dt.hour
        
        features = {
            'http_requests': len(df),
            'http_night_ratio': float(((df['hour'] >= 22) | (df['hour'] < 6)).mean()),
            'http_requests_per_day': float(len(df) / max(df['date'].dt.date.nunique(), 1))
        }
        
        if 'url' in df.columns:
            features['http_unique_urls'] = df['url'].nunique()
            features['http_url_diversity'] = float(df['url'].nunique() / max(len(df), 1))
            url_counts = df['url'].value_counts()
            features['http_max_url_freq'] = float(url_counts.max() / len(df)) if len(url_counts) > 0 else 0.0
        
        if 'pc' in df.columns:
            features['http_pc_count'] = df['pc'].nunique()
        
        return features
    
    def _extract_email_features(self, records):
        """提取Email聚合特征"""
        if not records or len(records) < 3:
            return {}
        
        df = pd.DataFrame(records)
        df['date'] = pd.to_datetime(df['date'])
        df['hour'] = df['date'].dt.hour
        
        features = {
            'email_count': len(df),
            'email_night_ratio': float(((df['hour'] >= 22) | (df['hour'] < 6)).mean()),
            'email_per_day': float(len(df) / max(df['date'].dt.date.nunique(), 1))
        }
        
        if 'to' in df.columns:
            features['email_sent'] = int(df['to'].notna().sum())
            features['email_received'] = int(df['to'].isna().sum())
            features['email_unique_to'] = df['to'].nunique()
            features['email_sent_ratio'] = float(features['email_sent'] / max(len(df), 1))
        
        if 'size' in df.columns:
            features['email_avg_size'] = float(df['size'].mean())
            features['email_max_size'] = float(df['size'].max())
        
        return features
    
    def _classify_user_risk(self, score):
        """用户级别风险分类（基于实验#10的分位数）"""
        # 这些阈值需要在训练时确定，这里使用估计值
        # 实际部署时应该从模型训练过程中保存真实阈值
        if score > -0.05:
            return '正常', '>75%'
        elif score > -0.15:
            return '低风险', '25%-75%'
        elif score > -0.25:
            return '中风险', '5%-25%'
        else:
            return '高风险', '<5%'
    
    def _classify_event_risk(self, score):
        """事件级别风险分类"""
        if score < 0.3:
            return '正常'
        elif score < 0.5:
            return '低风险'
        elif score < 0.7:
            return '中风险'
        else:
            return '高风险'


# ===== API 路由 =====

@app.route('/health', methods=['GET'])
def health_check():
    """健康检查接口"""
    return jsonify({
        'status': 'healthy',
        'model_loaded': detector is not None,
        'timestamp': datetime.now().isoformat()
    })

@app.route('/model/info', methods=['GET'])
def model_info():
    """获取模型信息"""
    if detector is None:
        return jsonify({'error': '模型未加载'}), 400
    
    return jsonify({
        'feature_count': len(detector.feature_columns),
        'feature_names': detector.feature_columns,
        'model_type': 'IncrementalOCSVM',
        'experiment': '#10 (最优配置)',
        'expected_performance': {
            'accuracy': 0.8160,
            'precision': 0.2625,
            'recall': 0.9000,
            'f1_score': 0.4065
        }
    })

@app.route('/detect/comprehensive', methods=['POST'])
def comprehensive_detection():
    """
    综合检测接口 - 同时返回用户级别和事件级别异常分数
    
    请求格式:
    {
        "user": "ACM2278",
        "behavior_data": {
            "logon_records": [
                {"date": "2010-01-04 08:30:00", "pc": "PC-1234"},
                ...
            ],
            "device_records": [...],
            "file_records": [...],
            "http_records": [...],
            "email_records": [...]
        }
    }
    
    响应格式:
    {
        "user": "ACM2278",
        "user_level": {
            "anomaly_score": -0.3542,
            "risk_level": "高风险",
            "is_anomaly": true
        },
        "event_level": {
            "logon_events": [...],
            "file_events": [...],
            ...
        },
        "summary": {
            "total_events_analyzed": 150,
            "anomaly_events_count": 12,
            "anomaly_rate": 0.08,
            "top_anomaly_events": [...]
        }
    }
    """
    try:
        if detector is None:
            return jsonify({'error': '模型未加载'}), 400
        
        data = request.json
        user_id = data.get('user')
        behavior_data = data.get('behavior_data', {})
        
        if not user_id:
            return jsonify({'error': '缺少用户ID'}), 400
        
        # 执行综合检测
        result = detector.comprehensive_detection(user_id, behavior_data)
        
        return jsonify(result)
        
    except Exception as e:
        return jsonify({
            'error': str(e),
            'traceback': traceback.format_exc()
        }), 500

@app.route('/detect/user_level', methods=['POST'])
def user_level_detection():
    """
    用户级别检测接口 - 只返回用户整体异常分数
    
    请求格式: 同 comprehensive
    响应格式: 只返回 user_level 部分
    """
    try:
        if detector is None:
            return jsonify({'error': '模型未加载'}), 400
        
        data = request.json
        print("UEBA input:", data)
        user_id = data.get('user')
        behavior_data = data.get('behavior_data', {})
        
        if not user_id:
            return jsonify({'error': '缺少用户ID'}), 400
        
        # 只执行用户级别检测
        result = detector._detect_user_level(user_id, behavior_data)
        result['timestamp'] = datetime.now().isoformat()
        
        return jsonify(result)
        
    except Exception as e:
        return jsonify({
            'error': str(e),
            'traceback': traceback.format_exc()
        }), 500

@app.route('/detect/event_level', methods=['POST'])
def event_level_detection():
    """
    事件级别检测接口 - 只返回每个事件的异常分数
    
    请求格式: 同 comprehensive
    响应格式: 只返回 event_level 部分
    """
    try:
        if detector is None:
            return jsonify({'error': '模型未加载'}), 400
        
        data = request.json
        print("UEBA input:", data)
        user_id = data.get('user')
        behavior_data = data.get('behavior_data', {})
        
        if not user_id:
            return jsonify({'error': '缺少用户ID'}), 400
        
        # 只执行事件级别检测
        result = detector._detect_event_level(user_id, behavior_data)
        
        return jsonify({
            'user': user_id,
            'event_level': result,
            'timestamp': datetime.now().isoformat()
        })
        
    except Exception as e:
        return jsonify({
            'error': str(e),
            'traceback': traceback.format_exc()
        }), 500

@app.route('/detect/single_event', methods=['POST'])
def single_event_detection():
    """
    单个事件检测接口
    
    请求格式:
    {
        "user": "ACM2278",
        "event_type": "logon",  // logon, device, file, http, email
        "event": {
            "date": "2010-01-05 23:30:00",
            "pc": "PC-9999"
        },
        "historical_data": {  // 可选：用于建立基线
            "logon_records": [...]
        }
    }
    """
    try:
        if detector is None:
            return jsonify({'error': '模型未加载'}), 400
        
        data = request.json
        print("UEBA input:", data)
        user_id = data.get('user')
        event_type = data.get('event_type')
        event = data.get('event')
        historical_data = data.get('historical_data', {})
        
        if not user_id or not event_type or not event:
            return jsonify({'error': '缺少必需参数'}), 400
        
        # 建立基线
        profile = detector._build_user_profile(historical_data)
        
        # 根据类型检测
        if event_type == 'logon':
            result = detector._detect_single_logon(event, profile.get('logon'))
        elif event_type == 'device':
            result = detector._detect_single_device(event, profile.get('device'))
        elif event_type == 'file':
            result = detector._detect_single_file(event, profile.get('file'))
        elif event_type == 'http':
            result = detector._detect_single_http(event, profile.get('http'))
        elif event_type == 'email':
            result = detector._detect_single_email(event, profile.get('email'))
        else:
            return jsonify({'error': f'不支持的事件类型: {event_type}'}), 400
        
        result['user'] = user_id
        result['event_type'] = event_type
        result['event'] = event
        result['timestamp'] = datetime.now().isoformat()
        
        return jsonify(result)
        
    except Exception as e:
        return jsonify({
            'error': str(e),
            'traceback': traceback.format_exc()
        }), 500


def main():
    """启动API服务"""
    import argparse
    
    parser = argparse.ArgumentParser(description='UEBA异常检测API服务')
    parser.add_argument('--model', required=True, help='模型文件路径')
    parser.add_argument('--host', default='0.0.0.0', help='服务地址')
    parser.add_argument('--port', type=int, default=5004, help='服务端口')
    
    args = parser.parse_args()
    
    # 检查模型文件是否存在
    if not os.path.exists(args.model):
        print(f"错误: 模型文件不存在: {args.model}")
        sys.exit(1)
    
    print("=" * 70)
    print("UEBA异常检测API服务")
    print("=" * 70)
    print(f"模型文件: {args.model}")
    print(f"服务地址: http://{args.host}:{args.port}")
    print("=" * 70)
    
    # 加载模型
    global detector
    detector = UnifiedDetectionSystem(args.model)
    
    print("\n可用接口:")
    print("  GET  /health                  - 健康检查")
    print("  GET  /model/info              - 模型信息")
    print("  POST /detect/comprehensive    - 综合检测（用户+事件）")
    print("  POST /detect/user_level       - 用户级别检测")
    print("  POST /detect/event_level      - 事件级别检测")
    print("  POST /detect/single_event     - 单个事件检测")
    print("\n" + "=" * 70)
    print("服务启动中...")
    print("=" * 70)
    
    # 启动Flask服务
    app.run(host=args.host, port=args.port, debug=False)


if __name__ == '__main__':
    main()
