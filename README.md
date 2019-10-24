# ICVLC Sender

> Inter-Car Visible Light Communication (Sender)

사용자로부터 음성 데이터를 받아 텍스트로 변환하여 페어링된 블루투스 기기에 데이터를 전송하는 안드로이드 애플리케이션


## Usage

**아두이노에 연결된 블루투스 모듈 이름 설경**

이름 : `li-fi`

> 이름을 이용해 자동으로 블루투스를 연결하기 때문에 사용 전, 반드시 블루투스 기기 이름을 설정하여야 한다.

**블루투스 페어링 진행**

현 프로젝트에는 자동으로 페어링을 하는 기능이 없으므로, 핸드폰의 `블루투스 > 등록된 디바이스` 메뉴에 `li-fi`가 등록되어야 한다.

## Library

* [Naver Clova CSR](https://docs.ncloud.com/ko/naveropenapi_v3/speech/recognition-sdk.html#UsingAndroidAPI)

## License

[MIT](https://github.com/icvlc/emitter/blob/master/LICENSE)
