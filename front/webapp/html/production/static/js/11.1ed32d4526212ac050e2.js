webpackJsonp([11],{HThm:function(t,i){},IZIm:function(t,i){},luiU:function(t,i,s){"use strict";Object.defineProperty(i,"__esModule",{value:!0});var e=s("mvHQ"),a=s.n(e),o=s("Dd8w"),n=s.n(o),r=s("fxnj"),c=s.n(r),d=s("NYxO"),l=s("KeBu"),h=s("S8Wg"),u=s("4FCr"),g=(h.a,u.a,l.a,n()({},Object(d.c)(["getadressLb"]),{checkAll:function(){return this.dataList.length===this.listAll.length}}),n()({},Object(d.b)(["getShingCart","addItemToShoppingCart","addWish","deleteCartItem","addressList","chooseCartItem"]),{goToDe:function(t){c.a.miniProgram.navigateTo({url:"/pages/goodDetail/index?id="+t})},reLoad:function(){this.showLoading(),this.getShingCart({data:{cartType:"wishCart"},this:this,method:this.getShingCart})},ontouchstart:function(t){this.sy=t.changedTouches[0].pageY},ontouchend:function(t){this.ey=t.changedTouches[0].pageY,this.sy-this.ey>30&&this.onScrollBottom()},ontouchstart1:function(t){this.sy=t.changedTouches[0].pageX},ontouchend1:function(t){this.ey=t.changedTouches[0].pageX,this.sy-this.ey>30&&(t.currentTarget.className="addLeft"),this.ey-this.sy>10&&(t.currentTarget.className="")},del:function(t){this.showLoading(),this.deleteCartItem({data:{product_ids:t,cartType:"wishCart"},this:this})},goToIndex:function(){c.a.miniProgram.switchTab({url:"/pages/home/index"})},toHlp:function(){var t=this;if(this.listAll.length){var i=this.dataList.filter(function(i){return t.listAll.indexOf(i.productId)>-1});console.log(i),c.a.miniProgram.navigateTo({url:"/pages/helpFriends/index?orderType="+a()(i)})}},addCart:function(t){c.a.miniProgram.navigateTo({url:"/pages/sureorder/index?orderType=wishCart&cartType=wishCart&wishId="+t})},ziBuy:function(){var t=this;if(this.listAll.length){var i=this.dataList.filter(function(i){return t.listAll.indexOf(i.productId)>-1}).map(function(t){return t.productId});this.addWish({this:this,data:{productIds:i.join(),contactMechId:this.getadressLb.defaultAddressId},method:this.addWish})}else this.showTost("请选择商品")},goTo:function(t){c.a.miniProgram.navigateTo({url:"/pages/sureorder/index?orderType=wishCart&cartType=wishCart&wishId="+t})},addMe:function(t){var i=this,s=this.dataList.map(function(t){return i.listAll.indexOf(t.productId)>-1?{product_id:t.productId,isChoose:"Y"}:{product_id:t.productId,isChoose:"N"}});s.push({cartType:"wishCart"}),this.showLoading(),this.chooseCartItem({data:a()(s),this:this,method:this.chooseCartItem})},getData:function(t){var i=this;this.flag=!0,this.listAll=[],this.dataList=t.filter(function(t){return!t.isGift||"N"==t.isGift}),this.zpData=t.filter(function(t){return"Y"==t.isGift}),this.dataList.forEach(function(t){"Y"==t.isChoose&&i.listAll.push(t.productId)})},isSal:function(){var t=this;this.dataList.length===this.listAll.length?this.listAll=[]:(this.listAll=[],this.dataList.forEach(function(i){t.listAll.push(i.productId)})),this.addMe()},handleEvents:function(t){console.log("event: ",t)},onScrollBottom:function(){var t=this;this.onFetching||(this.onFetching=!0,setTimeout(function(){t.$nextTick(function(){}),t.onFetching=!1},2e3))}}),{name:"toberealized",data:function(){return{zpData:[],flag:!1,onFetching:!1,listAll:[],dataList:[],sy:"",ey:""}},created:function(){this.$route.query.token&&localStorage.setItem("token",decodeURIComponent(this.$route.query.token)),this.$route.query.refreshToken&&localStorage.setItem("refreshToken",decodeURIComponent(this.$route.query.refreshToken)),this.showLoading(),this.getShingCart({data:{cartType:"wishCart"},this:this,method:this.getShingCart}),this.addressList({this:this,method:this.addressList})},mounted:function(){console.log(this.getadressLb)},components:{Scroller:h.a,LoadMore:u.a,empty:l.a},computed:n()({},Object(d.c)(["getadressLb"]),{checkAll:function(){return this.dataList.length===this.listAll.length}}),watch:{},methods:n()({},Object(d.b)(["getShingCart","addItemToShoppingCart","addWish","deleteCartItem","addressList","chooseCartItem"]),{goToDe:function(t){c.a.miniProgram.navigateTo({url:"/pages/goodDetail/index?id="+t})},reLoad:function(){this.showLoading(),this.getShingCart({data:{cartType:"wishCart"},this:this,method:this.getShingCart})},ontouchstart:function(t){this.sy=t.changedTouches[0].pageY},ontouchend:function(t){this.ey=t.changedTouches[0].pageY,this.sy-this.ey>30&&this.onScrollBottom()},ontouchstart1:function(t){this.sy=t.changedTouches[0].pageX},ontouchend1:function(t){this.ey=t.changedTouches[0].pageX,this.sy-this.ey>30&&(t.currentTarget.className="addLeft"),this.ey-this.sy>10&&(t.currentTarget.className="")},del:function(t){this.showLoading(),this.deleteCartItem({data:{product_ids:t,cartType:"wishCart"},this:this})},goToIndex:function(){c.a.miniProgram.switchTab({url:"/pages/home/index"})},toHlp:function(){var t=this;if(this.listAll.length){var i=this.dataList.filter(function(i){return t.listAll.indexOf(i.productId)>-1});console.log(i),c.a.miniProgram.navigateTo({url:"/pages/helpFriends/index?orderType="+a()(i)})}},addCart:function(t){c.a.miniProgram.navigateTo({url:"/pages/sureorder/index?orderType=wishCart&cartType=wishCart&wishId="+t})},ziBuy:function(){var t=this;if(this.listAll.length){var i=this.dataList.filter(function(i){return t.listAll.indexOf(i.productId)>-1}).map(function(t){return t.productId});this.addWish({this:this,data:{productIds:i.join(),contactMechId:this.getadressLb.defaultAddressId},method:this.addWish})}else this.showTost("请选择商品")},goTo:function(t){c.a.miniProgram.navigateTo({url:"/pages/sureorder/index?orderType=wishCart&cartType=wishCart&wishId="+t})},addMe:function(t){var i=this,s=this.dataList.map(function(t){return i.listAll.indexOf(t.productId)>-1?{product_id:t.productId,isChoose:"Y"}:{product_id:t.productId,isChoose:"N"}});s.push({cartType:"wishCart"}),this.showLoading(),this.chooseCartItem({data:a()(s),this:this,method:this.chooseCartItem})},getData:function(t){var i=this;this.flag=!0,this.listAll=[],this.dataList=t.filter(function(t){return!t.isGift||"N"==t.isGift}),this.zpData=t.filter(function(t){return"Y"==t.isGift}),this.dataList.forEach(function(t){"Y"==t.isChoose&&i.listAll.push(t.productId)})},isSal:function(){var t=this;this.dataList.length===this.listAll.length?this.listAll=[]:(this.listAll=[],this.dataList.forEach(function(i){t.listAll.push(i.productId)})),this.addMe()},handleEvents:function(t){console.log("event: ",t)},onScrollBottom:function(){var t=this;this.onFetching||(this.onFetching=!0,setTimeout(function(){t.$nextTick(function(){}),t.onFetching=!1},2e3))}})}),f={render:function(){var t=this,i=t.$createElement,e=t._self._c||i;return t.dataList.length?e("div",{staticStyle:{height:"calc(100% - 0.8rem)"}},[e("div",{ref:"content",staticClass:"css64",staticStyle:{overflow:"hidden"}},[e("ul",{staticClass:"yLb"},[t._l(t.zpData,function(i,s){return e("li",{key:i.productId+s,staticStyle:{"margin-left":"-0.2rem","padding-left":"0.8rem","background-color":"#eee"},on:{click:function(s){t.goToDe(i.productId)}}},[e("div",{staticClass:"imgkk"},[e("img",{staticClass:"goods_img",attrs:{src:i.mediumImageUrl}})]),t._v(" "),e("div",{staticClass:"textlist"},[e("p",[t._v(t._s(i.name))]),t._v(" "),t._m(0,!0)])])}),t._v(" "),t._l(t.dataList,function(i,s){return e("li",{key:i.productId+s,on:{touchend:function(i){i.stopPropagation(),t.ontouchend1(i)},touchstart:function(i){i.stopPropagation(),t.ontouchstart1(i)}}},[e("label",{staticClass:"checkCon",class:{active:-1!==t.listAll.indexOf(i.productId)},attrs:{for:i.productId}},[e("span",{staticClass:"dg"}),t._v(" "),e("input",{directives:[{name:"model",rawName:"v-model",value:t.listAll,expression:"listAll"}],attrs:{id:i.productId,type:"checkbox"},domProps:{value:i.productId,checked:Array.isArray(t.listAll)?t._i(t.listAll,i.productId)>-1:t.listAll},on:{change:[function(s){var e=t.listAll,a=s.target,o=!!a.checked;if(Array.isArray(e)){var n=i.productId,r=t._i(e,n);a.checked?r<0&&(t.listAll=e.concat([n])):r>-1&&(t.listAll=e.slice(0,r).concat(e.slice(r+1)))}else t.listAll=o},t.addMe]}})]),t._v(" "),e("div",{staticClass:"imgkk",on:{click:function(s){t.goToDe(i.productId)}}},[e("img",{staticClass:"goods_img",attrs:{src:i.mediumImageUrl}})]),t._v(" "),e("div",{staticClass:"textlist",on:{click:function(s){t.goToDe(i.productId)}}},[e("p",[t._v(t._s(i.name))]),t._v(" "),e("div",{staticClass:"flexbox justifyx",staticStyle:{"align-items":"flex-end"}},[e("div",t._l(i.features,function(s){return i.features?e("pre",{key:s.featureId,staticClass:"ll"},[t._v(t._s(s.productFeatureType)+"  "+t._s(s.productFeatureName))]):t._e()})),t._v(" "),e("span",{staticClass:"fred"},[t._v("￥"+t._s(i.displayPrice))])])]),t._v(" "),e("div",{staticClass:"del",on:{click:function(s){t.del(i.productId)}}},[t._v("\n          删除\n        ")])])})],2),t._v(" "),t.onFetching?e("load-more",{attrs:{tip:"loading"}}):t._e()],1),t._v(" "),e("div",{staticClass:"sfooter"},[e("div",{staticClass:"f1",on:{click:t.isSal}},[e("label",{staticClass:"checkCon",class:{active:t.checkAll}},[e("span",{staticClass:"dg"}),t._v(" "),e("span",[t._v("全选")])])]),t._v(" "),e("div",{staticClass:"f2",on:{click:t.ziBuy}},[t._v("购买")]),t._v(" "),e("div",{staticClass:"f3",on:{click:t.toHlp}},[t._v("求助好友")])])]):e("empty",{directives:[{name:"show",rawName:"v-show",value:t.flag,expression:"flag"}]},[e("img",{staticStyle:{width:"1.89rem",height:"1.91rem"},attrs:{slot:"img",src:s("OrkP"),alt:""},slot:"img"}),t._v(" "),e("button",{staticClass:"btnn  bred",attrs:{slot:"an"},on:{click:t.goToIndex},slot:"an"},[t._v("去首页")])])},staticRenderFns:[function(){var t=this.$createElement,i=this._self._c||t;return i("div",{staticClass:"flexbox justifyx",staticStyle:{"align-items":"flex-end"}},[i("div",{staticClass:"zppp"},[this._v("赠品")])])}]};var p=s("VU/8")(g,f,!1,function(t){s("HThm"),s("IZIm")},"data-v-6cfb9d1e",null);i.default=p.exports}});
//# sourceMappingURL=11.1ed32d4526212ac050e2.js.map