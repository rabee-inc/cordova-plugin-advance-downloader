/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

// Wait for the deviceready event before using any of Cordova's device APIs.
// See https://cordova.apache.org/docs/en/latest/cordova/events/events.html#deviceready
document.addEventListener('deviceready', onDeviceReady, false);
function onDeviceReady() {
    // permissions
    var permissions = cordova.plugins.permissions;
    permissions.requestPermissions([
        permissions.INTERNET,
        permissions.WRITE_EXTERNAL_STORAGE,
        permissions.READ_EXTERNAL_STORAGE
      ]);

    // button
    const button1 = document.querySelector('.button1')
    button1.addEventListener('click', listAction);

    const button2 = document.querySelector('.button2')
    button2.addEventListener('click', addAction);

    const button3 = document.querySelector('.button3')
    button3.addEventListener('click', startAction);

    const button4 = document.querySelector('.button4')
    button4.addEventListener('click', pauseAction);

    const button5 = document.querySelector('.button5')
    button5.addEventListener('click', stopAction);

    const button6 = document.querySelector('.button6')
    button6.addEventListener('click', getAction);
}

function listAction() {
    const addRequestParams = {}

    window.AdvanceDownloader.list(addRequestParams).then((v) => {
        window.alert(v)
    });
}

function addAction() {
    const addRequestParams = [
        {
            id: 'absfldsfsfjlsdfjs',
            url: 'https://dh2.v.netease.com/2017/cg/fxtpty.mp4',
            size: 123456789,
            path: '/downloads',
            name: 'fxtpty.mp4',
            headers: { // 認証ヘッダー
                auautherization: 'hoge.com',
            }

        },
        {
            id: 'hogefugafeaoiefjao',
            url: 'https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf',
            size: 123456789,
            path: '/downloads',
            name: 'dummy.pdf',
            headers: { // 認証ヘッダー
                auautherization: 'hoge.com',
            }

        },
        {
            id: 'fugapiyodafkjoaweifja',
            url: 'http://www.hochmuth.com/mp3/Haydn_Cello_Concerto_D-1.mp3',
            size: 123456789,
            path: '/downloads',
            name: 'Haydn_Cello_Concerto_D-1.mp3',
            headers: { // 認証ヘッダー
                auautherization: 'hoge.com',
            }

        },
    ]


    window.AdvanceDownloader.add(addRequestParams[Math.floor(Math.random() * addRequestParams.length)]).then((v) => {
        window.alert(v)
    });
}

function startAction() {
    const addRequestParams = {
        id: 'absfldsfsfjlsdfjs'
    }

    window.AdvanceDownloader.start(addRequestParams).then((v) => {
        window.alert(v)
    });
}

function pauseAction() {
    const addRequestParams = {
        id: 'absfldsfsfjlsdfjs'
    }

    window.AdvanceDownloader.resume(addRequestParams).then((v) => {
        window.alert(v)
    });
}

function stopAction() {
    const addRequestParams = {
        id: 'absfldsfsfjlsdfjs'
    }

    window.AdvanceDownloader.stop(addRequestParams).then((v) => {
        window.alert(v)
    });
}

function getAction() {
    const addRequestParams = {
        id: 'absfldsfsfjlsdfjs'
    }

    window.AdvanceDownloader.getTasks(addRequestParams).then((v) => {
        window.alert(v)
    });
}

