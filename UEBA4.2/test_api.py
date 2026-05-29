"""
API功能测试脚本
用于验证UEBA异常检测API是否正常工作
"""

import requests
import json
from datetime import datetime

BASE_URL = "http://localhost:5004"

def print_section(title):
    """打印分隔线"""
    print("\n" + "=" * 70)
    print(f"  {title}")
    print("=" * 70)

def test_health():
    """测试健康检查"""
    print_section("1. 健康检查")
    
    response = requests.get(f"{BASE_URL}/health")
    data = response.json()
    
    print(f"状态: {data.get('status')}")
    print(f"模型已加载: {data.get('model_loaded')}")
    print(f"时间: {data.get('timestamp')}")
    
    return response.status_code == 200

def test_model_info():
    """测试模型信息"""
    print_section("2. 模型信息")
    
    response = requests.get(f"{BASE_URL}/model/info")
    data = response.json()
    
    print(f"模型类型: {data.get('model_type')}")
    print(f"特征维度: {data.get('feature_count')}")
    print(f"实验版本: {data.get('experiment')}")
    print(f"\n预期性能:")
    perf = data.get('expected_performance', {})
    print(f"  准确率: {perf.get('accuracy')*100:.2f}%")
    print(f"  精确率: {perf.get('precision')*100:.2f}%")
    print(f"  召回率: {perf.get('recall')*100:.2f}%")
    print(f"  F1分数: {perf.get('f1_score'):.4f}")
    
    return response.status_code == 200

def test_user_level_detection():
    """测试用户级别检测"""
    print_section("3. 用户级别检测")
    
    # 准备测试数据
    request_data = {
        "user": "TEST_USER_001",
        "behavior_data": {
            "logon_records": [
                {"date": "2010-01-04 08:30:00", "pc": "PC-1234"},
                {"date": "2010-01-04 09:15:00", "pc": "PC-1234"},
                {"date": "2010-01-05 08:45:00", "pc": "PC-1234"},
                {"date": "2010-01-05 17:30:00", "pc": "PC-1234"},
                {"date": "2010-01-06 09:00:00", "pc": "PC-1234"},
                {"date": "2010-01-06 23:45:00", "pc": "PC-9999"},  # 异常：深夜+陌生设备
            ],
            "file_records": [
                {"date": "2010-01-05 10:00:00", "pc": "PC-1234", "filename": "report.pdf"},
                {"date": "2010-01-05 14:30:00", "pc": "PC-1234", "filename": "data.xlsx"},
                {"date": "2010-01-06 23:50:00", "pc": "PC-9999", "filename": "confidential.zip"},
            ],
            "email_records": [
                {"date": "2010-01-05 14:00:00", "to": "colleague@company.com", "size": 50000},
                {"date": "2010-01-07 02:45:00", "to": "external@gmail.com", "size": 50000000},  # 异常：深夜+大附件
            ]
        }
    }
    
    response = requests.post(
        f"{BASE_URL}/detect/user_level",
        json=request_data,
        headers={"Content-Type": "application/json"}
    )
    
    data = response.json()
    
    print(f"用户: {data.get('user')}")
    print(f"异常分数: {data.get('anomaly_score'):.4f}")
    print(f"风险等级: {data.get('risk_level')}")
    print(f"风险分位: {data.get('risk_percentile')}")
    print(f"是否异常: {'是' if data.get('is_anomaly') else '否'}")
    print(f"有效特征数: {data.get('features_count')}")
    
    return response.status_code == 200

def test_comprehensive_detection():
    """测试综合检测"""
    print_section("4. 综合检测（用户+事件）")
    
    request_data = {
        "user": "TEST_USER_002",
        "behavior_data": {
            "logon_records": [
                {"date": "2010-01-04 08:30:00", "pc": "PC-1234"},
                {"date": "2010-01-04 17:00:00", "pc": "PC-1234"},
                {"date": "2010-01-05 08:45:00", "pc": "PC-1234"},
                {"date": "2010-01-05 23:30:00", "pc": "PC-9999"},  # 异常
                {"date": "2010-01-06 02:00:00", "pc": "PC-9999"},  # 异常
            ],
            "file_records": [
                {"date": "2010-01-05 10:00:00", "pc": "PC-1234", "filename": "report.pdf"},
                {"date": "2010-01-05 23:45:00", "pc": "PC-9999", "filename": "secret.zip"},  # 异常
            ]
        }
    }
    
    response = requests.post(
        f"{BASE_URL}/detect/comprehensive",
        json=request_data,
        headers={"Content-Type": "application/json"}
    )
    
    data = response.json()
    
    # 用户级别结果
    user_level = data.get('user_level', {})
    print(f"\n【用户级别】")
    print(f"  异常分数: {user_level.get('anomaly_score'):.4f}")
    print(f"  风险等级: {user_level.get('risk_level')}")
    print(f"  是否异常: {'是' if user_level.get('is_anomaly') else '否'}")
    
    # 摘要
    summary = data.get('summary', {})
    print(f"\n【事件摘要】")
    print(f"  总事件数: {summary.get('total_events_analyzed')}")
    print(f"  异常事件数: {summary.get('anomaly_events_count')}")
    print(f"  异常率: {summary.get('anomaly_rate')*100:.2f}%")
    
    # 最异常的事件
    top_events = summary.get('top_anomaly_events', [])
    if top_events:
        print(f"\n【最可疑的事件（前3个）】")
        for i, event in enumerate(top_events[:3], 1):
            print(f"  {i}. [{event.get('type')}] 分数: {event.get('score'):.2f} | 风险: {event.get('risk')}")
            print(f"     事件: {event.get('event')}")
    
    return response.status_code == 200

def test_single_event_detection():
    """测试单个事件检测"""
    print_section("5. 单个事件实时检测")
    
    # 测试一个可疑的登录事件
    request_data = {
        "user": "TEST_USER_003",
        "event_type": "logon",
        "event": {
            "date": "2010-01-08 03:00:00",  # 凌晨3点
            "pc": "PC-UNKNOWN"  # 陌生设备
        },
        "historical_data": {
            "logon_records": [
                {"date": "2010-01-04 08:30:00", "pc": "PC-1234"},
                {"date": "2010-01-04 17:00:00", "pc": "PC-1234"},
                {"date": "2010-01-05 08:45:00", "pc": "PC-1234"},
                {"date": "2010-01-05 17:30:00", "pc": "PC-1234"},
            ]
        }
    }
    
    response = requests.post(
        f"{BASE_URL}/detect/single_event",
        json=request_data,
        headers={"Content-Type": "application/json"}
    )
    
    data = response.json()
    
    print(f"用户: {data.get('user')}")
    print(f"事件类型: {data.get('event_type')}")
    print(f"事件内容: {data.get('event')}")
    print(f"\n检测结果:")
    print(f"  异常分数: {data.get('anomaly_score'):.2f}")
    print(f"  风险等级: {data.get('risk_level')}")
    
    anomalies = data.get('anomalies', [])
    if anomalies:
        print(f"\n异常原因:")
        for anomaly in anomalies:
            print(f"  - {anomaly.get('type')} (严重度: {anomaly.get('severity')})")
    
    if data.get('anomaly_score', 0) > 0.5:
        print(f"\n⚠️ 警告：检测到可疑登录！建议采取措施。")
    
    return response.status_code == 200

def main():
    """运行所有测试"""
    print("\n" + "=" * 70)
    print("UEBA异常检测API功能测试")
    print("=" * 70)
    print(f"API地址: {BASE_URL}")
    print(f"测试时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    
    tests = [
        ("健康检查", test_health),
        ("模型信息", test_model_info),
        ("用户级别检测", test_user_level_detection),
        ("综合检测", test_comprehensive_detection),
        ("单事件检测", test_single_event_detection),
    ]
    
    results = []
    for test_name, test_func in tests:
        try:
            success = test_func()
            results.append((test_name, success))
        except Exception as e:
            print(f"\n❌ 测试失败: {e}")
            results.append((test_name, False))
    
    # 汇总结果
    print_section("测试结果汇总")
    
    passed = sum(1 for _, success in results if success)
    total = len(results)
    
    for test_name, success in results:
        status = "✓ 通过" if success else "✗ 失败"
        print(f"  {status}  {test_name}")
    
    print(f"\n总计: {passed}/{total} 测试通过")
    
    if passed == total:
        print("\n🎉 所有测试通过！API服务正常运行。")
    else:
        print(f"\n⚠️ {total - passed} 个测试失败，请检查API服务。")
    
    print("=" * 70)

if __name__ == "__main__":
    try:
        main()
    except requests.exceptions.ConnectionError:
        print("\n❌ 错误：无法连接到API服务")
        print(f"请确保API服务已启动：python3 ueba_api_service.py --model result/incremental_ocsvm_model.joblib")
    except Exception as e:
        print(f"\n❌ 测试失败: {e}")
        import traceback
        traceback.print_exc()
