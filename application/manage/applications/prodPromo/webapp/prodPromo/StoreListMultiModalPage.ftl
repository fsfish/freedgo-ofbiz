<style>
    .select-info {
        -moz-user-select: none;
        border: 1px solid #CECECE;
        border-radius: 3px 3px 3px 3px;
        box-shadow: 0 0 1px 1px #FFFFFF inset, 0 1px 5px rgba(0, 0, 0, 0.1);
        margin: 8px;
        overflow: hidden;
        padding: 14px 10px 14px 60px;
        position: relative;
    }

    .select-info .top-label {
        height: 20px;
        left: 5px;
        position: absolute;
        top: 20px;
        font-size: 14px;
        font-weight: bold;
        color: #5893B7;
    }

    .select-info li.current {
        background-color: #e4e4e4;
        border: 1px solid #aaa;
        border-radius: 4px;
        cursor: default;
        float: left;
        margin-right: 5px;
        margin-top: 5px;
        padding: 0 5px;
        background-color: #3c8dbc;
        border-color: #367fa9;
        padding: 1px 10px;
        color: #fff;
        list-style-type:none;
    }

    .select-info li.current span.icon-del{
        color: #999;
        cursor: pointer;
        display: inline-block;
        font-weight: bold;
        margin-right: 2px;
        margin-right: 5px;
        color: rgba(255,255,255,0.7);
    }

    .a-btn {
        -moz-transition: box-shadow 0.3s ease-in-out 0s;
        background: -moz-linear-gradient(center top , #FFFFFF 0%, #F6F6F6 74%, #EDEDED 100%) repeat scroll 0 0 transparent;
        border-radius: 6px 6px 6px 6px;
        box-shadow: 0 0 7px rgba(0, 0, 0, 0.2), 0 0 0 1px rgba(188, 188, 188, 0.1);
        display: block;
        float: right;
        margin: 2px;
        overflow: hidden;
        position: relative;
    }

    .a-btn-text {
        color: #3c8dbc;
        display: block;
        font-size: 12px;
        text-align: center;
        line-height: 16px;
        padding: 5px;
        text-shadow: 1px 1px 2px rgba(255, 255, 255, 0.5);
    }
</style>

<!-- 内容start -->
<div class="box box-info">
    <div class="box-body">
        <!-- 条件查询start -->
        <form id="PromoFM_QueryForm" class="form-inline clearfix">
            <div class="form-group">
                <div class="input-group m-b-10">
                    <span class="input-group-addon">店铺名称</span>
                    <input type="text" class="form-control" id="storeName">
                </div>
            </div>
            <div class="form-group">
                <div class="input-group m-b-10">
                    <span class="input-group-addon">商家编号</span>
                    <input type="text" class="form-control" id="partyId">
                </div>
            </div>
            <div class="input-group pull-right m-l-10">
                <button id="searchBtn" class="btn btn-success btn-flat">${uiLabelMap.BrandSearch}</button>
            </div>
        </form><!-- 条件查询end -->

        <!-- 分割线start -->
        <div class="cut-off-rule bg-gray"></div>
        <!-- 分割线end -->

        <!-- 选中结果区域start -->
        <div class="select-info" style="display: none;">
            <label class="top-label">已选：</label>
            <ul class="p-0">

            </ul>
            <a id="menu-confirm" href="javascript:void(0);" class="a-btn btn-select">
                <span class="a-btn-text">确定</span>
            </a>
        </div>
        <!-- 选中结果区域end -->

        <!-- 表格区域start -->
        <div class="row">
            <div class="col-sm-12">
                <table id="PromoFM_data_tbl" class="table table-bordered table-hover js-checkparent"></table>
            </div>
        </div><!-- 表格区域end -->

        <!-- 分页条start -->
        <div class="row" id="PromoFM_paginateDiv">
        </div><!-- 分页条end -->
    </div><!-- /.box-body -->
</div><!-- 内容end -->

<!-- script区域start -->
<script>
    var PromoFM_data_tbl;
    var PromoFM_ajaxUrl = "/prodPromo/control/StoreListForJson?VIEW_SIZE=5";
    $(function(){
        PromoFM_data_tbl = $('#PromoFM_data_tbl').dataTable({
            ajaxUrl: PromoFM_ajaxUrl,
            columns:[
                {"title":"复选框","code":"productStoreId","checked":"true",
                    "handle":function(td,record){
                        var checkbox = td.find('.js-checkchild:checkbox');
                        if(record.storeName) {
                            checkbox.attr({"data-name": record.storeName});
                        }else{
                            checkbox.attr({"data-name": record.partyId});
                        }
                        //根据选中结果集默认选中
                        $.each($('.select-info ul').find('li'),function(i,v){
                            if(checkbox.val() == $(v).attr("id")){
                                checkbox.prop("checked",true);
                            }
                        });
                    }
                },
                {"title":"店铺名称","code":"storeName"},
                {"title":"商家编码","code":"partyId"},
                {"title":"品牌LOGO","code":"imgUrl",
                    "handle":function(td,record){
                        var img="";
                        if(record.imgUrl){
                            img = "<img class='img-responsive' style='max-height: 70px;max-width: 70px;' src='"+record.imgUrl+"'>";
                        }
                        var div =$("<div class='col-sm-12'  align='center'>"+img+"</div>");
                        td.html(div);
                    }
                }
            ],
            listName: "recordsList",
            paginateEL: "PromoFM_paginateDiv",
            headNotShow: true,
            midShowNum: 3
        });

        //查询按钮点击事件
        $('#PromoFM_QueryForm #searchBtn').on('click',function(){
            var storeName = $('#PromoFM_QueryForm #storeName').val();
            var partyId = $('#PromoFM_QueryForm #partyId').val();
            PromoFM_ajaxUrl = changeURLArg(PromoFM_ajaxUrl,"storeName",storeName);
            PromoFM_ajaxUrl = changeURLArg(PromoFM_ajaxUrl,"partyId",partyId);
            PromoFM_data_tbl.reload(PromoFM_ajaxUrl);
            return false;
        });
    });


</script><!-- script区域end -->
